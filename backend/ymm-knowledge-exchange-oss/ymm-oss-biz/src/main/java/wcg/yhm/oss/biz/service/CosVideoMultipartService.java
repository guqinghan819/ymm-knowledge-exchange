package wcg.yhm.oss.biz.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.oss.biz.config.cos.CosProperties;
import wcg.yhm.oss.biz.config.cos.CosVideoMultipartProperties;
import wcg.yhm.oss.biz.domain.dto.VideoMultipartInitRequest;
import wcg.yhm.oss.biz.domain.model.CosVideoMultipartSession;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartInitResponse;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartPartResponse;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartSignResponse;
import wcg.yhm.oss.biz.domain.vo.VideoMultipartStatusResponse;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${strategy.type:}' == 'cos' and ${cos.video.multipart.enabled:false}")
public class CosVideoMultipartService {

    private static final String SESSION_KEY_PREFIX = "oss:video:multipart:";
    private static final String LOCK_KEY_PREFIX = "oss:video:multipart:lock:";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("video/mp4", "video/quicktime");
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final COSClient cosClient;
    private final CosProperties cosProperties;
    private final CosVideoMultipartProperties multipartProperties;
    private final StringRedisTemplate redisTemplate;

    public VideoMultipartInitResponse initialize(VideoMultipartInitRequest request) {
        Long userId = requireUserId();
        ValidatedVideo video = validateVideo(request);
        String taskId = UUID.randomUUID().toString();
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String objectKey = String.format("video/%s/%s/%s.%s", userId, date, UUID.randomUUID(), video.extension());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(video.contentType());
        InitiateMultipartUploadRequest cosRequest = new InitiateMultipartUploadRequest(
                cosProperties.getBucket(), objectKey, metadata);

        InitiateMultipartUploadResult result;
        try {
            result = cosClient.initiateMultipartUpload(cosRequest);
        } catch (CosClientException e) {
            log.error("COS初始化视频分片任务失败, userId={}, objectKey={}", userId, objectKey, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        }

        long partSize = multipartProperties.getPartSizeBytes();
        int totalParts = Math.toIntExact((video.fileSize() + partSize - 1) / partSize);
        CosVideoMultipartSession session = new CosVideoMultipartSession();
        session.setTaskId(taskId);
        session.setUserId(userId);
        session.setUploadId(result.getUploadId());
        session.setObjectKey(objectKey);
        session.setOriginalFileName(request.getFileName());
        session.setFileSize(video.fileSize());
        session.setContentType(video.contentType());
        session.setLastModified(request.getLastModified());
        session.setPartSize(partSize);
        session.setTotalParts(totalParts);
        session.setStatus(CosVideoMultipartSession.ACTIVE);

        try {
            saveSession(session);
        } catch (RuntimeException e) {
            abortQuietly(session);
            throw e;
        }
        return new VideoMultipartInitResponse(taskId, partSize, totalParts, session.getExpiresAt());
    }

    public VideoMultipartSignResponse signPart(String taskId, int partNumber) {
        CosVideoMultipartSession session = getOwnedSession(taskId);
        requireActive(session);
        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PART_ERROR);
        }

        long expiresAt = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(multipartProperties.getSignatureTtlSeconds());
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosProperties.getBucket(), session.getObjectKey(), HttpMethodName.PUT);
        request.setExpiration(new Date(expiresAt));
        request.addRequestParameter("uploadId", session.getUploadId());
        request.addRequestParameter("partNumber", String.valueOf(partNumber));
        try {
            URL url = cosClient.generatePresignedUrl(request);
            touchSession(session);
            return new VideoMultipartSignResponse(url.toString(), expiresAt);
        } catch (CosClientException e) {
            log.error("生成COS视频分片签名失败, taskId={}, partNumber={}", taskId, partNumber, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        }
    }

    public VideoMultipartStatusResponse getStatus(String taskId) {
        CosVideoMultipartSession session = getOwnedSession(taskId);
        if (CosVideoMultipartSession.COMPLETED.equals(session.getStatus())) {
            return completedStatus(session);
        }
        try {
            List<PartSummary> parts = listAllParts(session);
            touchSession(session);
            return activeStatus(session, parts);
        } catch (CosServiceException e) {
            return handleMissingUploadForStatus(session, e);
        } catch (CosClientException e) {
            log.error("查询COS视频分片失败, taskId={}", taskId, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        }
    }

    public String complete(String taskId) {
        CosVideoMultipartSession initial = getOwnedSession(taskId);
        if (CosVideoMultipartSession.COMPLETED.equals(initial.getStatus())) {
            return initial.getResultUrl();
        }

        String lockKey = LOCK_KEY_PREFIX + taskId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                lockKey, lockValue, Duration.ofSeconds(multipartProperties.getCompleteLockSeconds()));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_STATE_ERROR);
        }

        try {
            CosVideoMultipartSession session = getOwnedSession(taskId);
            if (CosVideoMultipartSession.COMPLETED.equals(session.getStatus())) {
                return session.getResultUrl();
            }
            requireActive(session);

            List<PartSummary> parts;
            try {
                parts = listAllParts(session);
            } catch (CosServiceException e) {
                if (isNoSuchUpload(e) && objectExists(session)) {
                    return markCompleted(session);
                }
                throw e;
            }
            validateCompleteParts(session, parts);
            // The COS SDK sorts this list in place while building the completion XML.
            List<PartETag> partETags = new ArrayList<>(parts.stream()
                    .sorted(Comparator.comparingInt(PartSummary::getPartNumber))
                    .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                    .toList());
            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                    cosProperties.getBucket(), session.getObjectKey(), session.getUploadId(), partETags);
            cosClient.completeMultipartUpload(request);
            return markCompleted(session);
        } catch (BizException e) {
            throw e;
        } catch (CosClientException e) {
            log.error("合并COS视频分片失败, taskId={}", taskId, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        } finally {
            releaseLockQuietly(lockKey, lockValue);
        }
    }

    public void abort(String taskId) {
        CosVideoMultipartSession session = getOwnedSession(taskId);
        requireActive(session);
        try {
            cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(
                    cosProperties.getBucket(), session.getObjectKey(), session.getUploadId()));
            redisTemplate.delete(sessionKey(taskId));
        } catch (CosServiceException e) {
            if (isNoSuchUpload(e)) {
                redisTemplate.delete(sessionKey(taskId));
                return;
            }
            log.error("取消COS视频分片任务失败, taskId={}", taskId, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        } catch (CosClientException e) {
            log.error("取消COS视频分片任务失败, taskId={}", taskId, e);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        }
    }

    private VideoMultipartStatusResponse handleMissingUploadForStatus(
            CosVideoMultipartSession session, CosServiceException exception) {
        if (!isNoSuchUpload(exception)) {
            log.error("查询COS视频分片失败, taskId={}", session.getTaskId(), exception);
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_OPERATION_FAIL);
        }
        if (objectExists(session)) {
            markCompleted(session);
            return completedStatus(session);
        }
        redisTemplate.delete(sessionKey(session.getTaskId()));
        throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_TASK_EXPIRED);
    }

    private List<PartSummary> listAllParts(CosVideoMultipartSession session) {
        List<PartSummary> result = new ArrayList<>();
        Integer marker = null;
        do {
            ListPartsRequest request = new ListPartsRequest(
                    cosProperties.getBucket(), session.getObjectKey(), session.getUploadId());
            request.setMaxParts(1000);
            if (marker != null) {
                request.setPartNumberMarker(marker);
            }
            PartListing listing = cosClient.listParts(request);
            result.addAll(listing.getParts());
            marker = listing.isTruncated() ? listing.getNextPartNumberMarker() : null;
        } while (marker != null);
        return result;
    }

    private void validateCompleteParts(CosVideoMultipartSession session, List<PartSummary> parts) {
        if (parts.size() != session.getTotalParts()) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_INCOMPLETE);
        }
        Set<Integer> partNumbers = new HashSet<>();
        for (PartSummary part : parts) {
            if (!partNumbers.add(part.getPartNumber()) ||
                    part.getPartNumber() < 1 || part.getPartNumber() > session.getTotalParts()) {
                throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_INCOMPLETE);
            }
            long expectedSize = expectedPartSize(session, part.getPartNumber());
            if (part.getSize() != expectedSize) {
                throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_INCOMPLETE);
            }
        }
        for (int number = 1; number <= session.getTotalParts(); number++) {
            if (!partNumbers.contains(number)) {
                throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_INCOMPLETE);
            }
        }
    }

    private long expectedPartSize(CosVideoMultipartSession session, int partNumber) {
        if (partNumber < session.getTotalParts()) {
            return session.getPartSize();
        }
        long remainder = session.getFileSize() % session.getPartSize();
        return remainder == 0 ? session.getPartSize() : remainder;
    }

    private VideoMultipartStatusResponse activeStatus(
            CosVideoMultipartSession session, List<PartSummary> parts) {
        List<VideoMultipartPartResponse> uploadedParts = parts.stream()
                .sorted(Comparator.comparingInt(PartSummary::getPartNumber))
                .map(part -> new VideoMultipartPartResponse(part.getPartNumber(), part.getSize()))
                .toList();
        long uploadedBytes = parts.stream().mapToLong(PartSummary::getSize).sum();
        return new VideoMultipartStatusResponse(session.getStatus(), uploadedParts, uploadedBytes,
                session.getFileSize(), session.getExpiresAt(), null);
    }

    private VideoMultipartStatusResponse completedStatus(CosVideoMultipartSession session) {
        return new VideoMultipartStatusResponse(session.getStatus(), List.of(), session.getFileSize(),
                session.getFileSize(), session.getExpiresAt(), session.getResultUrl());
    }

    private String markCompleted(CosVideoMultipartSession session) {
        String url = cosProperties.getHost() + "/" + session.getObjectKey();
        session.setStatus(CosVideoMultipartSession.COMPLETED);
        session.setResultUrl(url);
        saveSession(session);
        return url;
    }

    private boolean objectExists(CosVideoMultipartSession session) {
        try {
            return cosClient.doesObjectExist(cosProperties.getBucket(), session.getObjectKey());
        } catch (CosClientException e) {
            return false;
        }
    }

    private CosVideoMultipartSession getOwnedSession(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_TASK_EXPIRED);
        }
        String json = redisTemplate.opsForValue().get(sessionKey(taskId));
        if (!StringUtils.hasText(json)) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_TASK_EXPIRED);
        }
        CosVideoMultipartSession session = JsonUtil.parseObject(json, CosVideoMultipartSession.class);
        Long userId = requireUserId();
        if (!userId.equals(session.getUserId())) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_FORBIDDEN);
        }
        return session;
    }

    private void requireActive(CosVideoMultipartSession session) {
        if (!CosVideoMultipartSession.ACTIVE.equals(session.getStatus())) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_STATE_ERROR);
        }
    }

    private void saveSession(CosVideoMultipartSession session) {
        session.setExpiresAt(System.currentTimeMillis() +
                TimeUnit.HOURS.toMillis(multipartProperties.getTaskTtlHours()));
        redisTemplate.opsForValue().set(sessionKey(session.getTaskId()), JsonUtil.toJsonString(session),
                Duration.ofHours(multipartProperties.getTaskTtlHours()));
    }

    private void touchSession(CosVideoMultipartSession session) {
        saveSession(session);
    }

    private String sessionKey(String taskId) {
        return SESSION_KEY_PREFIX + taskId;
    }

    private Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_FORBIDDEN);
        }
        return userId;
    }

    private ValidatedVideo validateVideo(VideoMultipartInitRequest request) {
        if (request == null || !StringUtils.hasText(request.getFileName()) || request.getFileSize() == null) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        long fileSize = request.getFileSize();
        if (fileSize < multipartProperties.getThresholdBytes() ||
                fileSize > multipartProperties.getMaxFileSizeBytes()) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        String cleanName = StringUtils.cleanPath(request.getFileName());
        if (cleanName.contains("..") || cleanName.contains("/") || cleanName.contains("\\")) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        int dot = cleanName.lastIndexOf('.');
        if (dot < 1 || dot == cleanName.length() - 1) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        String extension = cleanName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = request.getContentType() == null
                ? "" : request.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType) ||
                ("mp4".equals(extension) && !"video/mp4".equals(contentType)) ||
                ("mov".equals(extension) && !"video/quicktime".equals(contentType))) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        long partSize = multipartProperties.getPartSizeBytes();
        if (partSize < 1024L * 1024 || partSize > 5L * 1024 * 1024 * 1024) {
            throw new BizException(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR);
        }
        return new ValidatedVideo(fileSize, extension, contentType);
    }

    private boolean isNoSuchUpload(CosServiceException exception) {
        return "NoSuchUpload".equals(exception.getErrorCode()) || exception.getStatusCode() == 404;
    }

    private void abortQuietly(CosVideoMultipartSession session) {
        try {
            cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(
                    cosProperties.getBucket(), session.getObjectKey(), session.getUploadId()));
        } catch (Exception e) {
            log.warn("Redis写入失败后清理COS分片任务失败, taskId={}", session.getTaskId(), e);
        }
    }

    private void releaseLockQuietly(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
        } catch (RuntimeException e) {
            log.warn("释放视频分片合并锁失败, lockKey={}", lockKey, e);
        }
    }

    private record ValidatedVideo(long fileSize, String extension, String contentType) {
    }
}
