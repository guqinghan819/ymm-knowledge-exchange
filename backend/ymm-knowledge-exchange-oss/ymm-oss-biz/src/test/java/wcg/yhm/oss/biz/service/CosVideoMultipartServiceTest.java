package wcg.yhm.oss.biz.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.internal.RequestXmlFactory;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListPartsRequest;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.oss.biz.config.cos.CosProperties;
import wcg.yhm.oss.biz.config.cos.CosVideoMultipartProperties;
import wcg.yhm.oss.biz.domain.dto.VideoMultipartInitRequest;
import wcg.yhm.oss.biz.domain.model.CosVideoMultipartSession;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosVideoMultipartServiceTest {

    private static final long PART_SIZE = 8L * 1024 * 1024;
    private static final long FILE_SIZE = 20L * 1024 * 1024;

    @Mock
    private COSClient cosClient;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private CosVideoMultipartService service;
    private CosVideoMultipartProperties multipartProperties;

    @BeforeEach
    void setUp() {
        CosProperties cosProperties = new CosProperties();
        cosProperties.setBucket("test-bucket-123");
        cosProperties.setHost("https://test.cos.example.com");

        multipartProperties = new CosVideoMultipartProperties();
        multipartProperties.setThresholdBytes(FILE_SIZE);
        multipartProperties.setMaxFileSizeBytes(500L * 1024 * 1024);
        multipartProperties.setPartSizeBytes(PART_SIZE);
        multipartProperties.setSignatureTtlSeconds(900);
        multipartProperties.setTaskTtlHours(24);
        multipartProperties.setCompleteLockSeconds(30);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new CosVideoMultipartService(cosClient, cosProperties, multipartProperties, redisTemplate);
        UserContextHolder.setUserId(7L);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.removeThreadLocal();
    }

    @Test
    void initializesMultipartUploadAndPersistsOwnedSession() {
        InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
        result.setUploadId("cos-upload-id");
        when(cosClient.initiateMultipartUpload(any())).thenReturn(result);

        VideoMultipartInitRequest request = new VideoMultipartInitRequest();
        request.setFileName("demo.mp4");
        request.setFileSize(FILE_SIZE);
        request.setContentType("video/mp4");
        request.setLastModified(123L);

        var response = service.initialize(request);

        assertThat(response.getPartSize()).isEqualTo(PART_SIZE);
        assertThat(response.getTotalParts()).isEqualTo(3);
        ArgumentCaptor<InitiateMultipartUploadRequest> cosRequest =
                ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
        verify(cosClient).initiateMultipartUpload(cosRequest.capture());
        assertThat(cosRequest.getValue().getKey()).matches("video/7/\\d{4}-\\d{2}-\\d{2}/.+\\.mp4");
        assertThat(cosRequest.getValue().getObjectMetadata().getContentType()).isEqualTo("video/mp4");

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(anyString(), json.capture(), eq(Duration.ofHours(24)));
        CosVideoMultipartSession session = JsonUtil.parseObject(json.getValue(), CosVideoMultipartSession.class);
        assertThat(session.getUserId()).isEqualTo(7L);
        assertThat(session.getUploadId()).isEqualTo("cos-upload-id");
    }

    @Test
    void rejectsUnsupportedVideoBeforeCallingCos() {
        VideoMultipartInitRequest request = new VideoMultipartInitRequest();
        request.setFileName("demo.avi");
        request.setFileSize(FILE_SIZE);
        request.setContentType("video/x-msvideo");

        assertThatThrownBy(() -> service.initialize(request))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.VIDEO_MULTIPART_PARAM_ERROR.getErrorCode()));
        verify(cosClient, never()).initiateMultipartUpload(any());
    }

    @Test
    void signsOnlyValidPartNumbers() throws Exception {
        stubSession(activeSession(7L));
        when(cosClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL("https://test.cos.example.com/signed"));

        var response = service.signPart("task-1", 2);

        assertThat(response.getUploadUrl()).isEqualTo("https://test.cos.example.com/signed");
        ArgumentCaptor<GeneratePresignedUrlRequest> request =
                ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(cosClient).generatePresignedUrl(request.capture());
        assertThat(request.getValue().getRequestParameters())
                .containsEntry("uploadId", "upload-1")
                .containsEntry("partNumber", "2");

        assertThatThrownBy(() -> service.signPart("task-1", 4))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.VIDEO_MULTIPART_PART_ERROR.getErrorCode()));
    }

    @Test
    void preventsAnotherUserFromReadingTask() {
        stubSession(activeSession(99L));

        assertThatThrownBy(() -> service.getStatus("task-1"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.VIDEO_MULTIPART_FORBIDDEN.getErrorCode()));
        verify(cosClient, never()).listParts(any(ListPartsRequest.class));
    }

    @Test
    void reportsExpiredWhenRedisSessionIsMissing() {
        when(valueOperations.get("oss:video:multipart:missing-task")).thenReturn(null);

        assertThatThrownBy(() -> service.getStatus("missing-task"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.VIDEO_MULTIPART_TASK_EXPIRED.getErrorCode()));
        verify(cosClient, never()).listParts(any(ListPartsRequest.class));
    }

    @Test
    void returnsCompletedResultIdempotentlyWithoutCallingCosAgain() {
        CosVideoMultipartSession session = activeSession(7L);
        session.setStatus(CosVideoMultipartSession.COMPLETED);
        session.setResultUrl("https://test.cos.example.com/video/7/file.mp4");
        stubSession(session);

        String url = service.complete("task-1");

        assertThat(url).isEqualTo(session.getResultUrl());
        verify(cosClient, never()).listParts(any(ListPartsRequest.class));
        verify(cosClient, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void refusesToCompleteWhenAnyPartIsMissing() {
        stubSession(activeSession(7L));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(cosClient.listParts(any(ListPartsRequest.class)))
                .thenReturn(partListing(part(1, PART_SIZE), part(2, PART_SIZE)));

        assertThatThrownBy(() -> service.complete("task-1"))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getErrorCode())
                                .isEqualTo(ErrorCodeEnum.VIDEO_MULTIPART_INCOMPLETE.getErrorCode()));
        verify(cosClient, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void completesAllPartsInOrderAndReturnsPublicUrl() {
        stubSession(activeSession(7L));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(cosClient.listParts(any(ListPartsRequest.class)))
                .thenReturn(partListing(part(3, 4L * 1024 * 1024), part(1, PART_SIZE), part(2, PART_SIZE)));

        String url = service.complete("task-1");

        assertThat(url).isEqualTo("https://test.cos.example.com/video/7/file.mp4");
        ArgumentCaptor<CompleteMultipartUploadRequest> request =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(cosClient).completeMultipartUpload(request.capture());
        assertThat(request.getValue().getPartETags())
                .extracting(partETag -> partETag.getPartNumber())
                .containsExactly(1, 2, 3);
        String xml = new String(RequestXmlFactory.convertToXmlByteArray(request.getValue().getPartETags()),
                StandardCharsets.UTF_8);
        assertThat(xml).contains("<PartNumber>1</PartNumber>", "<PartNumber>2</PartNumber>",
                "<PartNumber>3</PartNumber>");
    }

    @Test
    void abortsCosTaskAndDeletesRedisSession() {
        stubSession(activeSession(7L));

        service.abort("task-1");

        verify(cosClient).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        verify(redisTemplate).delete("oss:video:multipart:task-1");
    }

    private void stubSession(CosVideoMultipartSession session) {
        when(valueOperations.get("oss:video:multipart:task-1"))
                .thenReturn(JsonUtil.toJsonString(session));
    }

    private CosVideoMultipartSession activeSession(Long userId) {
        CosVideoMultipartSession session = new CosVideoMultipartSession();
        session.setTaskId("task-1");
        session.setUserId(userId);
        session.setUploadId("upload-1");
        session.setObjectKey("video/7/file.mp4");
        session.setOriginalFileName("file.mp4");
        session.setFileSize(FILE_SIZE);
        session.setContentType("video/mp4");
        session.setPartSize(PART_SIZE);
        session.setTotalParts(3);
        session.setStatus(CosVideoMultipartSession.ACTIVE);
        return session;
    }

    private PartListing partListing(PartSummary... parts) {
        PartListing listing = new PartListing();
        listing.setParts(List.of(parts));
        listing.setTruncated(false);
        return listing;
    }

    private PartSummary part(int number, long size) {
        PartSummary part = new PartSummary();
        part.setPartNumber(number);
        part.setSize(size);
        part.setETag("etag-" + number);
        return part;
    }
}
