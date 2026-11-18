package wcg.yhm.note.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.note.constants.MQConstant;
import wcg.yhm.note.constants.RedisKeyConstant;
import wcg.yhm.note.api.domain.event.NoteRagIndexMessage;
import wcg.yhm.note.api.domain.response.NoteRagIndexDetailResponse;
import wcg.yhm.note.domain.dataobject.*;
import wcg.yhm.note.domain.mapper.NoteCollectionMapper;
import wcg.yhm.note.domain.mapper.NoteLikeMapper;
import wcg.yhm.note.domain.mapper.NoteMapper;
import wcg.yhm.note.enums.*;
import wcg.yhm.note.model.dto.*;
import wcg.yhm.note.model.vo.AdminNoteItemResponse;
import wcg.yhm.note.model.vo.ChannelWordCloudItemResponse;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.note.model.vo.MyNoteItemResponse;
import wcg.yhm.note.model.vo.MyNotesPageResponse;
import wcg.yhm.note.model.vo.NoteAnalysisNoteItemResponse;
import wcg.yhm.note.model.vo.NoteAnalysisPeriodResponse;
import wcg.yhm.note.model.vo.NoteAnalysisResponse;
import wcg.yhm.note.model.vo.NoteItemResponse;
import wcg.yhm.note.model.vo.UserInteractionSummaryResponse;
import wcg.yhm.note.rpc.*;
import wcg.yhm.note.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.note.utils.CaffeineUtil;
import wcg.yhm.user.api.domain.vo.CreatorResponse;
import wcg.yhm.user.api.domain.vo.FindUserResponse;
import wcg.yhm.user.relation.api.controller.UserRelationFeignApi;
import wcg.yhm.user.relation.api.domain.response.FollowResultResponse;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.CursorPageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToLongFunction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 笔记表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {

    @Resource
    private NoteAiReviewTaskService noteAiReviewTaskService;

    @Resource
    private NoteRagOutboxService noteRagOutboxService;

    @Resource
    private NoteNotificationOutboxService noteNotificationOutboxService;

    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

    @Resource
    private TextRpcService textRpcService;

    @Resource
    private ITopicService topicService;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private CountRpcService countRpcService;

    @Resource
    private UserRelationRpcService userRelationRpcService;

    @Resource
    @Lazy
    private INoteLikeService noteLikeService;

    @Resource
    @Lazy
    private INoteCollectionService noteCollectionService;

    @Resource
    private IChannelTopicRelService channelTopicRelService;

    @Resource
    private IChannelService channelService;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void publishNote(PublishNoteRequest publishNoteRequest) {
        if (publishNoteRequest == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //1.判断笔记类型
        Integer type = publishNoteRequest.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.getNoteTypeEnum(type);
        if (ObjectUtil.isNull(noteTypeEnum)) {
            throw new BizException(ErrorCodeEnum.NOTE_TYPE_ERROR);
        }
        String imageUrls = null;
        boolean isContentEmpty = true; //默认文本内容为空
        String videoUrl = null;

        switch (noteTypeEnum) {
            case IMAGE:
                List<String> imageUrlList = publishNoteRequest.getImageUrls();
                if (imageUrlList.size() > 8) {
                    throw new BizException(ErrorCodeEnum.NOTE_IMAGE_COUNT_ERROR);
                }
                //以逗号进行拼接
                imageUrls = StrUtil.join(",", imageUrlList);
                break;
            case VIDEO:
                videoUrl = publishNoteRequest.getVideoUrl(); //video 地址
                if (StrUtil.isBlank(videoUrl)) {
                    throw new BizException(ErrorCodeEnum.NOTE_VIDEO_URL_ERROR);
                }
                break;
            default:
                break;
        }
        //2.获取雪花id
        String noteId = distributedIdGeneratorRpcService.getPartitionKey();
        //分区键
        String noteContentUUID = null;
        //3.调用text文本存储服务进行保存
        String content = publishNoteRequest.getContent();
        if (StrUtil.isNotBlank(content)) {
            isContentEmpty = false;
            noteContentUUID = UUID.randomUUID().toString();
            //保存
            boolean result = textRpcService.saveNoteContent(noteContentUUID, content);
            if (!result) {
                throw new BizException(ErrorCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }
        //查询话题
        Long topicId = publishNoteRequest.getTopicId();
        String topicName = null;
        if (ObjectUtil.isNotEmpty(topicId)) {
            Topic topic = topicService.getById(topicId);
            topicName = topic.getName();
        }
        //获取当前登陆用户
        Long loginUserId = UserContextHolder.getUserId();
        //4.封装实体类
        Note note = new Note();
        note.setId(Long.valueOf(noteId));
        note.setTitle(publishNoteRequest.getTitle());
        note.setImgUris(imageUrls);
        note.setIsContentEmpty(isContentEmpty);
        note.setVideoUri(videoUrl);
        note.setIsTop(Boolean.FALSE);
        note.setTopicName(topicName);
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        note.setType(type);
        note.setCreatorId(loginUserId);
        note.setStatus(NoteStatusEnum.PENDING_REVIEW.getValue());
        note.setVisible(NoteVisibleEnum.PUBLIC.getValue());
        note.setTopicId(topicId);
        note.setContentUuid(noteContentUUID);
        note.setReviewVersion(1);
        //5. 插入
        boolean result = save(note);
        if (!result) {
            //如果失败就删除文本内容
            if (StrUtil.isNotBlank(noteContentUUID)) {
                boolean success = textRpcService.deleteNoteContent(noteContentUUID);
            }
            throw new BizException(ErrorCodeEnum.NOTE_PUBLISH_FAIL);
        }
        // 图文笔记随事务写入 AI 审核任务；视频笔记保持待审，交给管理员处理。
        if (Objects.equals(type, NoteTypeEnum.IMAGE.getValue())) {
            noteAiReviewTaskService.enqueue(note.getId(), 1);
        }
        //发送发布笔记计数服务
        MQPublishCountDTO mqPublishCountDTO =
                new MQPublishCountDTO(loginUserId, NotePublishEnum.PUBLISH.getCode(), LocalDateTime.now());
        String destination = MQConstant.TOPIC_COUNT_NOTE_PUBLISH + ":" + MQConstant.TAG_PUBLISH;
        Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(mqPublishCountDTO)).build();
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=======> 用户发布笔记计数消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 用户发布笔记计数消息发送失败, error: ", throwable);
            }
        });

    }

    @Override
    public FindNoteDetailResponse findNoteDetail(FindNoteDetailRequest findNoteDetailRequest) {
        return loadNoteDetail(findNoteDetailRequest, false);
    }

    @Override
    public FindNoteDetailResponse adminNoteDetail(FindNoteDetailRequest findNoteDetailRequest) {
        return loadNoteDetail(findNoteDetailRequest, true);
    }

    private FindNoteDetailResponse loadNoteDetail(FindNoteDetailRequest findNoteDetailRequest, boolean admin) {
        Long noteId = findNoteDetailRequest.getId();
        if (noteId < 0) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Long userId = UserContextHolder.getUserId();
        //构建redis key
        String noteKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        //查询本地缓存（跳过含null字段的旧缓存）
        FindNoteDetailResponse noteDetail = admin ? null : CaffeineUtil.getNoteDetail(noteId);
        if (ObjectUtil.isNotNull(noteDetail)
                && ObjectUtil.isNotNull(noteDetail.getIsTop())
                && ObjectUtil.isNotNull(noteDetail.getStatus())) {
            if (!admin) checkNoteAccessible(userId, noteDetail.getCreatorId(), noteDetail.getStatus(), noteDetail.getVisible());
            log.info("================> 本地缓存命中: {}", noteDetail);
            return noteDetail;
        }
        //查询redis（跳过含null字段的旧缓存）
        Object cachedNote = admin ? null : redisTemplate.opsForValue().get(noteKey);
        String noteStr = cachedNote == null ? null : String.valueOf(cachedNote);
        if (StrUtil.isNotBlank(noteStr) && !"null".equals(noteStr)) {
            FindNoteDetailResponse response = JsonUtil.parseObject(noteStr, FindNoteDetailResponse.class);
            if (response != null
                    && ObjectUtil.isNotNull(response.getIsTop())
                    && ObjectUtil.isNotNull(response.getStatus())) {
                if (!admin) checkNoteAccessible(userId, response.getCreatorId(), response.getStatus(), response.getVisible());
                threadPoolTaskExecutor.submit(() -> {
                    CaffeineUtil.saveNoteDetail(noteId, noteStr);
                });
                return response;
            }
        }
        //在db中查询笔记（作者本人可查看任意状态的笔记）
        Note note = lambdaQuery().eq(Note::getId, noteId).one();
        if (ObjectUtil.isNull(note)) {
            //缓存空值
            threadPoolTaskExecutor.execute(() -> {
                long expireSecond = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteKey, "null", expireSecond, TimeUnit.SECONDS);
            });
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        //判断笔记是否可见
        if (!admin) checkNoteAccessible(userId, note.getCreatorId(), note.getStatus(), note.getVisible());
        //获取创作者信息
        //异步并发查询优化
        Long creatorId = note.getCreatorId();
        CompletableFuture<CreatorResponse> userResultFuture = CompletableFuture
                .supplyAsync(() -> userRpcService.getCreator(creatorId), threadPoolTaskExecutor);
        //获取笔记内容文本信息,如果存在
        CompletableFuture<String> contentResultFuture = CompletableFuture.completedFuture(null);
        if (BooleanUtil.isFalse(note.getIsContentEmpty())) {
            contentResultFuture = CompletableFuture
                    .supplyAsync(() -> textRpcService.getContent(note.getContentUuid()), threadPoolTaskExecutor);
        }
        //这里重新创建一个带final的，不然会检查出错误
        CompletableFuture<String> finalContentResultFuture = contentResultFuture;
        CompletableFuture<FindNoteDetailResponse> resultFuture = CompletableFuture
                .allOf(userResultFuture, contentResultFuture) //等待所有异步任务完成
                .thenApply(s -> {
                    CreatorResponse creator = userResultFuture.join();
                    String content = finalContentResultFuture.join();
                    //如果是图文笔记，对图片地址进行处理
                    List<String> imgUris = null;
                    if (note.getType().equals(NoteTypeEnum.IMAGE.getValue())) {
                        imgUris = List.of(note.getImgUris().split(","));
                    }
                    //封装
                    FindNoteDetailResponse response = BeanUtil.copyProperties(note, FindNoteDetailResponse.class, "imgUris");
                    response.setCreatorName(creator.getNickname());
                    response.setImgUris(imgUris);
                    response.setContent(content);
                    response.setAvatar(creator.getAvatar());
                    // 显式设置 BeanUtil 可能遗漏的布尔字段
                    response.setIsTop(note.getIsTop());
                    response.setStatus(note.getStatus());
                    if (admin) {
                        Map<String, Object> task = noteAiReviewTaskService.latest(noteId, note.getReviewVersion());
                        response.setReviewTaskStatus((String) task.get("status"));
                        response.setReviewInternalError((String) task.get("internal_error"));
                    } else {
                        response.setReviewInternalError(null);
                    }
                    // 查询频道信息
                    if (ObjectUtil.isNotNull(note.getTopicId())) {
                        ChannelTopicRel rel = channelTopicRelService.lambdaQuery()
                                .eq(ChannelTopicRel::getTopicId, note.getTopicId())
                                .orderByAsc(ChannelTopicRel::getChannelId)
                                .last("LIMIT 1")
                                .one();
                        if (ObjectUtil.isNotNull(rel)) {
                            response.setChannelId(rel.getChannelId());
                            Channel channel = channelService.getById(rel.getChannelId());
                            if (ObjectUtil.isNotNull(channel)) {
                                response.setChannelName(channel.getName());
                            }
                        }
                    }
                    return response;
                });

        FindNoteDetailResponse response;
        try {
            response = resultFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("查询笔记详情异步任务被中断, noteId: {}", noteId, e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BizException bizException) {
                throw bizException;
            }
            log.error("查询笔记详情异步任务执行失败, noteId: {}", noteId, cause);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        //存入redis中
        if (!admin) {
            threadPoolTaskExecutor.submit(() -> {
                long expireSecond = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);
                redisTemplate.opsForValue()
                        .set(noteKey, JsonUtil.toJsonString(response), expireSecond, TimeUnit.SECONDS);
            });
        }
        return response;
    }

    @Override
    public NoteRagIndexDetailResponse getRagIndexDetail(Long noteId) {
        if (Objects.isNull(noteId) || noteId < 0) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Note note = getById(noteId);
        if (Objects.isNull(note)) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }

        NoteRagIndexDetailResponse response = new NoteRagIndexDetailResponse();
        response.setNoteId(note.getId());
        response.setTitle(note.getTitle());
        response.setContentUuid(note.getContentUuid());
        response.setTopicId(note.getTopicId());
        response.setTopicName(note.getTopicName());
        response.setCreatorId(note.getCreatorId());
        response.setType(note.getType());
        response.setVisible(note.getVisible());
        response.setStatus(note.getStatus());
        response.setCreateTime(note.getCreateTime());
        response.setUpdateTime(note.getUpdateTime());

        if (BooleanUtil.isFalse(note.getIsContentEmpty()) && StrUtil.isNotBlank(note.getContentUuid())) {
            response.setContent(textRpcService.getContent(note.getContentUuid()));
        }
        if (StrUtil.isNotBlank(note.getImgUris())) {
            response.setCover(note.getImgUris().split(",")[0]);
        }

        CreatorResponse creator = userRpcService.getCreator(note.getCreatorId());
        if (Objects.nonNull(creator)) {
            response.setCreatorName(creator.getNickname());
            response.setCreatorAvatar(creator.getAvatar());
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateNote(UpdateNoteRequest updateNoteRequest) {
        if (updateNoteRequest == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }

        //查询笔记
        Long noteId = updateNoteRequest.getId();
        if (noteId < 0) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Note note = getById(noteId);
        if (Objects.isNull(note)) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }

        //判断权限
        Long userId = UserContextHolder.getUserId();
        if (!Objects.equals(userId, note.getCreatorId())) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_AUTH);
        }
        Integer type = updateNoteRequest.getType();
        if (type.equals(NoteTypeEnum.IMAGE.getValue())) {
            List<String> imgUris = updateNoteRequest.getImgUris();
            if (imgUris.size() > 8) {
                throw new BizException(ErrorCodeEnum.NOTE_IMAGE_COUNT_ERROR);
            }
        }
        Long topicId = updateNoteRequest.getTopicId();
        Note updateNote = BeanUtil.copyProperties(updateNoteRequest, Note.class, "imgUris");
        // imgUris 在 Note 中是逗号分隔的字符串，在 UpdateNoteRequest 中是 List<String>
        // BeanUtil 直接 copy 会调用 List.toString() 产生 "[url1, url2]" 带方括号
        if (CollUtil.isNotEmpty(updateNoteRequest.getImgUris())) {
            updateNote.setImgUris(StrUtil.join(",", updateNoteRequest.getImgUris()));
        }
        if (Objects.nonNull(topicId)) {
            Topic topic = topicService.getById(topicId);
            updateNote.setTopicName(topic.getName());
        }
        updateNote.setUpdateTime(LocalDateTime.now());

        String updateContent = updateNoteRequest.getContent();
        // 每次编辑都生成新的正文 UUID，避免正在执行的审核读到下一版本的内容。
        if (StrUtil.isBlank(updateContent)) {
            updateNote.setIsContentEmpty(Boolean.TRUE);
            updateNote.setContentUuid("");
        } else {
            String contentUUID = UUID.randomUUID().toString();
            if (!textRpcService.saveNoteContent(contentUUID, updateContent)) {
                throw new BizException(ErrorCodeEnum.NOTE_UPDATE_FAIL);
            }
            updateNote.setContentUuid(contentUUID);
            updateNote.setIsContentEmpty(Boolean.FALSE);
        }

        //删除缓存
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        redisTemplate.delete(redisKey);
        //更新数据库
        // 编辑后立即回到待审并提升版本号；旧版本的 AI 结果将无法更新此笔记。
        int nextVersion = (note.getReviewVersion() == null ? 1 : note.getReviewVersion()) + 1;
        boolean result = lambdaUpdate()
                .set(Note::getTitle, updateNote.getTitle())
                .set(Note::getType, updateNote.getType())
                .set(Note::getImgUris, updateNote.getImgUris())
                .set(Note::getVideoUri, updateNote.getVideoUri())
                .set(Note::getTopicId, updateNote.getTopicId())
                .set(Note::getTopicName, updateNote.getTopicName())
                .set(Note::getVisible, updateNote.getVisible())
                .set(Note::getContentUuid, updateNote.getContentUuid())
                .set(Note::getIsContentEmpty, updateNote.getIsContentEmpty())
                .set(Note::getStatus, NoteStatusEnum.PENDING_REVIEW.getValue())
                .set(Note::getReviewMethod, null)
                .set(Note::getReviewReason, null)
                .set(Note::getReviewedAt, null)
                .set(Note::getReviewVersion, nextVersion)
                .set(Note::getUpdateTime, updateNote.getUpdateTime())
                .eq(Note::getId, noteId)
                .eq(Note::getStatus, note.getStatus())
                .eq(Note::getReviewVersion, note.getReviewVersion())
                .update();
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_UPDATE_FAIL);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, updateNote.getUpdateTime());
        if (Objects.equals(type, NoteTypeEnum.IMAGE.getValue())) {
            noteAiReviewTaskService.enqueue(noteId, nextVersion);
        }

        //使用延迟双删保证数据一致性，mq发送延迟消息
        Message<String> message = MessageBuilder.withPayload(String.valueOf(noteId)).build();
        rocketMQTemplate.asyncSend(MQConstant.TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("## 延迟删除 Redis 笔记缓存消息发送成功...");
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 延迟删除 Redis 笔记缓存消息发送失败...", throwable);
            }
        }, 3000, 1); //超时时间3秒，延迟等级设置为1，表示延迟1秒(每个延迟等级不同，延时时间也不同)
        //本地缓存如果不进行消息广播，会造成缓存数据不一致(笔记服务集群还会存留本地缓存)
        //使用消息队列进行广播
        //CaffeineUtil.deleteNoteDetail(noteId);
        //同步广播
        rocketMQTemplate.syncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteNote(DeleteNoteRequest deleteNoteRequest) {
        if (deleteNoteRequest == null || deleteNoteRequest.getNoteId() == null || deleteNoteRequest.getNoteId() <= 0) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }

        Long noteId = deleteNoteRequest.getNoteId();
        Note n = getById(noteId);
        if (n == null) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        if (Objects.equals(n.getStatus(), NoteStatusEnum.DELETED.getValue())) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        Long userId = UserContextHolder.getUserId();
        if (!Objects.equals(userId, n.getCreatorId())) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_AUTH);
        }

        //进行逻辑删除
        boolean result = lambdaUpdate()
                .set(Note::getStatus, NoteStatusEnum.DELETED.getValue())
                .set(Note::getUpdateTime, LocalDateTime.now())
                .eq(Note::getId, noteId)
                .ne(Note::getStatus, NoteStatusEnum.DELETED.getValue())
                .update();
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, LocalDateTime.now());

        //删除redis
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        redisTemplate.delete(redisKey);

        //mq广播删除本地缓存
        rocketMQTemplate.syncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("====> MQ：删除笔记本地缓存发送成功...");
        //发送计数消息
        String destination = MQConstant.TOPIC_COUNT_NOTE_PUBLISH + ":" + MQConstant.TAG_DELETE;
        MQPublishCountDTO mqPublishCountDTO = new MQPublishCountDTO(userId, NotePublishEnum.DELETE.getCode(), LocalDateTime.now());
        Message<MQPublishCountDTO> message = MessageBuilder.withPayload(mqPublishCountDTO).build();
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=======> 发送用户删除笔记计数消息成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 发送用户删除笔记计数消息失败, error: ", throwable);
            }
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setVisibleOnly(SetVisibleRequest setVisibleRequest) {
        Long noteId = setVisibleRequest.getNoteId();
        Note note = new Note();
        note.setUpdateTime(LocalDateTime.now());
        note.setId(noteId);
        note.setVisible(NoteVisibleEnum.PRIVATE.getValue());

        Note n = getById(noteId);
        if (n == null) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        Long userId = UserContextHolder.getUserId();
        if (!Objects.equals(userId, n.getCreatorId())) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_AUTH);
        }

        boolean result = updateById(note);
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_SET_VISIBLE_FAIL);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, note.getUpdateTime());

        //删除redis 缓存
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        redisTemplate.delete(redisKey);

        //广播删除本地缓存
        rocketMQTemplate.syncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("=======> 删除本地缓存消息发送成功...");
    }

    @Override
    public void setTop(SetTopRequest setTopRequest) {
        Boolean isTop = setTopRequest.getIsTop();
        Long noteId = setTopRequest.getNoteId();
        Note note = getById(noteId);
        if (note == null) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //判断当前登陆用户是否是作者
        if (!userId.equals(note.getCreatorId())) {
            throw new BizException(ErrorCodeEnum.NOTE_SET_TOP_NO_AUTH);
        }

        Note updateNote = new Note();
        updateNote.setId(noteId);
        updateNote.setUpdateTime(LocalDateTime.now());
        updateNote.setIsTop(isTop);

        boolean result = updateById(updateNote);
        if (!result) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR);
        }

        //删除redis 缓存
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        redisTemplate.delete(redisKey);

        //广播删除本地缓存
        rocketMQTemplate.syncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
        log.info("=======> 删除本地缓存消息发送成功...");
    }

    @Override
    public CursorPageResponse<NoteItemResponse> getNextPage(Long cursor, int limit, Long channelId, Long loginUserId) {
        List<Long> topicIds = null;
        if (Objects.nonNull(channelId)) {
            //将channel下面的话题查出来
            topicIds = channelTopicRelService.lambdaQuery().select(ChannelTopicRel::getTopicId)
                    .eq(ChannelTopicRel::getChannelId, channelId).list().stream().map(ChannelTopicRel::getTopicId).toList();

            if (topicIds.isEmpty()) {
                return CursorPageResponse.success(List.of(), null, false, 0);
            }
        }
        Long userId = UserContextHolder.getUserId();
        Long total = null;
        if (Objects.nonNull(loginUserId)) {
            total = lambdaQuery()
                    .eq(Note::getCreatorId, loginUserId)
                    .eq(Note::getStatus, NoteStatusEnum.NORMAL.getValue())
                    .count();
        }
        //多查一条判断还有没有数据
        //只包含笔记id,创作者id
        List<Note> notes = getBaseMapper().selectByCursor(cursor, limit + 1, topicIds, loginUserId);
        if (CollUtil.isEmpty(notes)) {
            if (Objects.nonNull(total)) {
                return CursorPageResponse.success(List.of(), null, false, 0, total);
            }
            return CursorPageResponse.success(List.of(), null, false, 0);
        }
        boolean hasNext = notes.size() > limit;
        Long nextCursor = null;

        if (hasNext) {
            //移除多余的那条数据
           notes = notes.subList(0, limit);
           //获取最后一条记录id作为游标
            Note note = notes.get(notes.size() - 1);
            nextCursor = note.getId();
        }
        //提取笔记id
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        //通过笔记id获取笔记计数
        List<NoteCountResponse> noteCountResponses = countRpcService.batchQueryNoteCount(noteIds);
        //将noteCountResponses 转换成map<noteId, count>
        Map<Long, NoteCountResponse> noteIdWithCountMap = null;
        if (noteCountResponses != null) {
            noteIdWithCountMap = noteCountResponses.stream()
                    .collect(Collectors.toMap(NoteCountResponse::getNoteId, noteCount -> noteCount));
        }
        List<NoteItemResponse> noteItemResponseList = new ArrayList<>();
        //通过笔记id获取笔记的详细情况
        for (Long id : noteIds) {
            FindNoteDetailResponse noteDetail = findCursorNoteDetail(id, userId);
            if (noteDetail == null) {
                continue;
            }
            NoteItemResponse noteItemResponse = new NoteItemResponse();
            noteItemResponse.setFindNoteDetailResponse(noteDetail);
            noteItemResponseList.add(noteItemResponse);
        }
        if (CollUtil.isNotEmpty(noteIdWithCountMap)) {
            //封装
            Map<Long, NoteCountResponse> finalNoteIdWithCountMap = noteIdWithCountMap;
            noteItemResponseList.forEach(noteItem -> {
                Long id = noteItem.getFindNoteDetailResponse().getId();
                if (finalNoteIdWithCountMap.containsKey(id)) {
                    noteItem.setNoteCountResponse(finalNoteIdWithCountMap.get(id));
                }
            });
        }



        //校验关注: 将笔记中的创作者id提取出来
        List<Long> creatorIds = notes.stream()
                .map(Note::getCreatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<FollowResultResponse> resultResponses = userRelationRpcService.checkFollow(userId, creatorIds);
        Map<Long, FollowResultResponse> followUserIdWithResultMap = null;
        if (CollUtil.isNotEmpty(resultResponses)) {
            //转换成map
            followUserIdWithResultMap = resultResponses.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(FollowResultResponse::getCreatorId, result -> result,
                            (left, right) -> left));
        }
        if (CollUtil.isNotEmpty(followUserIdWithResultMap)) {
            Map<Long, FollowResultResponse> finalFollowUserIdWithResultMap = followUserIdWithResultMap;
            noteItemResponseList.forEach(item -> {
                Long creatorId = item.getFindNoteDetailResponse().getCreatorId();
                if (finalFollowUserIdWithResultMap.containsKey(creatorId)) {
                    item.setFollowing(finalFollowUserIdWithResultMap.get(creatorId).isFollow());
                }
            });
        }

        //校验笔记点赞:
        List<NoteLike> noteLikes = noteLikeService.lambdaQuery().eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus())
                .in(NoteLike::getNoteId, noteIds).list();
        //已经点赞笔记的id Set
        Set<Long> noteLikeIdsSet = noteLikes.stream().map(NoteLike::getNoteId).collect(Collectors.toSet());

        //校验笔记收藏:
        List<NoteCollection> noteCollections = noteCollectionService.lambdaQuery().eq(NoteCollection::getUserId, userId)
                .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode())
                .in(NoteCollection::getNoteId, noteIds).list();
        //已经收藏笔记的id Set
        Set<Long> noteCollectIdsSet = noteCollections.stream().map(NoteCollection::getNoteId).collect(Collectors.toSet());

        noteItemResponseList.forEach(item -> {
            Long noteId = item.getFindNoteDetailResponse().getId();
            if (CollUtil.isNotEmpty(noteLikeIdsSet) && noteLikeIdsSet.contains(noteId)) {
                item.setLike(true); //设置为点过赞
            }
            if (CollUtil.isNotEmpty(noteCollectIdsSet) && noteCollectIdsSet.contains(noteId)) {
                item.setCollect(true); //设置为已经收藏
            }
        });

        // 查询当前用户的笔记总数（仅当查询特定用户时）
        if (Objects.nonNull(loginUserId)) {
            return CursorPageResponse.success(noteItemResponseList, nextCursor, hasNext, noteItemResponseList.size(), total);
        }
        return CursorPageResponse.success(noteItemResponseList, nextCursor, hasNext, noteItemResponseList.size());
    }

    private FindNoteDetailResponse findCursorNoteDetail(Long noteId, Long userId) {
        try {
            return findNoteDetail(new FindNoteDetailRequest(noteId));
        } catch (BizException error) {
            if (!isUnavailableNote(error)) {
                throw error;
            }
            // 列表查询与详情加载之间笔记可能被下架；缓存也可能仍保留旧状态。
            Note current = getById(noteId);
            if (!canAccessNoteItem(current, userId)) {
                return null;
            }
            CaffeineUtil.deleteNoteDetail(noteId);
            redisTemplate.delete(RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId);
            try {
                return findNoteDetail(new FindNoteDetailRequest(noteId));
            } catch (BizException retryError) {
                if (isUnavailableNote(retryError)) {
                    return null;
                }
                throw retryError;
            }
        }
    }

    private boolean isUnavailableNote(BizException error) {
        return Objects.equals(error.getErrorCode(), ErrorCodeEnum.NOTE_NOT_FOUND.getErrorCode())
                || Objects.equals(error.getErrorCode(), ErrorCodeEnum.NOTE_PRIVATE.getErrorCode());
    }

    @Override
    public List<NoteItemResponse> batchQueryNoteItems(List<Long> noteIds) {
        if (CollUtil.isEmpty(noteIds)) {
            return List.of();
        }
        List<Long> distinctNoteIds = noteIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(distinctNoteIds)) {
            return List.of();
        }

        Long loginUserId = UserContextHolder.getUserId();
        List<Note> notes = lambdaQuery()
                .in(Note::getId, distinctNoteIds)
                .ne(Note::getStatus, NoteStatusEnum.DELETED.getValue())
                .list();
        if (CollUtil.isEmpty(notes)) {
            return List.of();
        }

        Map<Long, Note> accessibleNoteMap = notes.stream()
                .filter(note -> canAccessNoteItem(note, loginUserId))
                .collect(Collectors.toMap(Note::getId, note -> note, (left, right) -> left));
        if (CollUtil.isEmpty(accessibleNoteMap)) {
            return List.of();
        }

        List<Long> orderedIds = distinctNoteIds.stream()
                .filter(accessibleNoteMap::containsKey)
                .toList();
        Map<Long, NoteCountResponse> countMap = batchQueryNoteCountMap(orderedIds);
        Set<Long> likeNoteIds = queryLikedNoteIds(loginUserId, orderedIds);
        Set<Long> collectNoteIds = queryCollectedNoteIds(loginUserId, orderedIds);
        Map<Long, FollowResultResponse> followMap = queryFollowMap(loginUserId, accessibleNoteMap.values().stream()
                .map(Note::getCreatorId)
                .distinct()
                .toList());

        List<NoteItemResponse> responses = new ArrayList<>();
        for (Long noteId : orderedIds) {
            FindNoteDetailResponse noteDetail = findNoteDetail(new FindNoteDetailRequest(noteId));
            NoteItemResponse item = new NoteItemResponse();
            item.setFindNoteDetailResponse(noteDetail);
            item.setNoteCountResponse(countMap.getOrDefault(noteId, new NoteCountResponse()));
            item.setLike(likeNoteIds.contains(noteId));
            item.setCollect(collectNoteIds.contains(noteId));
            FollowResultResponse follow = followMap.get(noteDetail.getCreatorId());
            item.setFollowing(follow != null && follow.isFollow());
            responses.add(item);
        }
        return responses;
    }

    @Override
    public CursorPageResponse<NoteItemResponse> getCollectNote(Long cursor, int limit) {
        return getInteractionNotes(cursor, limit, false);
    }

    @Override
    public CursorPageResponse<NoteItemResponse> getLikeNote(Long cursor, int limit) {
        return getInteractionNotes(cursor, limit, true);
    }

    @Override
    public UserInteractionSummaryResponse getInteractionSummary() {
        Long userId = requireLoginUserId();
        String likeKey = RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY + userId;
        String collectKey = RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY + userId;
        warmInteractionZSet(userId, true, likeKey);
        warmInteractionZSet(userId, false, collectKey);
        return new UserInteractionSummaryResponse(
                (long) accessibleInteractionNoteIds(userId, likeKey).size(),
                (long) accessibleInteractionNoteIds(userId, collectKey).size());
    }

    private CursorPageResponse<NoteItemResponse> getInteractionNotes(Long cursor, int requestedLimit,
                                                                      boolean likeType) {
        Long userId = requireLoginUserId();
        int limit = Math.max(1, Math.min(requestedLimit, 50));
        long offset = cursor == null ? 0L : Math.max(cursor, 0L);
        String redisKey = (likeType ? RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY
                : RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY) + userId;

        warmInteractionZSet(userId, likeType, redisKey);
        redisTemplate.expire(redisKey, interactionCacheTtlSeconds(), TimeUnit.SECONDS);

        List<Long> accessibleIds = accessibleInteractionNoteIds(userId, redisKey);
        int start = (int) Math.min(offset, accessibleIds.size());
        int end = (int) Math.min((long) start + limit, accessibleIds.size());
        List<NoteItemResponse> records = batchQueryNoteItems(accessibleIds.subList(start, end));
        String likeKey = RedisKeyConstant.ZSET_USER_NOTE_LIKE_LIKE_KEY + userId;
        String collectKey = RedisKeyConstant.ZSET_USER_NOTE_COLLECT_KEY + userId;
        warmInteractionZSet(userId, true, likeKey);
        warmInteractionZSet(userId, false, collectKey);
        records.forEach(item -> {
            Long noteId = item.getFindNoteDetailResponse().getId();
            item.setLike(redisTemplate.opsForZSet().score(likeKey, noteId) != null);
            item.setCollect(redisTemplate.opsForZSet().score(collectKey, noteId) != null);
        });
        boolean hasNext = end < accessibleIds.size();
        Long nextCursor = hasNext ? (long) end : null;
        return CursorPageResponse.success(records, nextCursor, hasNext, records.size(), (long) accessibleIds.size());
    }

    private List<Long> accessibleInteractionNoteIds(Long userId, String redisKey) {
        long size = zSetSize(redisKey);
        List<Long> accessibleIds = new ArrayList<>();
        for (long offset = 0; offset < size; offset += 200) {
            Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(redisKey, offset, Math.min(offset + 199, size - 1));
            if (CollUtil.isEmpty(tuples)) {
                continue;
            }
            List<Long> noteIds = tuples.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(Long::valueOf)
                    .toList();
            Set<Long> visibleIds = lambdaQuery()
                    .select(Note::getId, Note::getCreatorId, Note::getStatus, Note::getVisible)
                    .in(Note::getId, noteIds)
                    .list().stream()
                    .filter(note -> canAccessNoteItem(note, userId))
                    .map(Note::getId)
                    .collect(Collectors.toSet());
            noteIds.stream().filter(visibleIds::contains).forEach(accessibleIds::add);
        }
        return accessibleIds;
    }

    private void warmInteractionZSet(Long userId, boolean likeType, String redisKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            return;
        }
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
        if (likeType) {
            noteLikeService.lambdaQuery()
                    .select(NoteLike::getNoteId, NoteLike::getCreateTime)
                    .eq(NoteLike::getUserId, userId)
                    .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus())
                    .list()
                    .forEach(item -> tuples.add(new DefaultTypedTuple<>(
                            item.getNoteId(),
                            (double) wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil
                                    .localDateTime2TimeStamp(item.getCreateTime()))));
        } else {
            noteCollectionService.lambdaQuery()
                    .select(NoteCollection::getNoteId, NoteCollection::getCreateTime)
                    .eq(NoteCollection::getUserId, userId)
                    .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode())
                    .list()
                    .forEach(item -> tuples.add(new DefaultTypedTuple<>(
                            item.getNoteId(),
                            (double) wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil
                                    .localDateTime2TimeStamp(item.getCreateTime()))));
        }
        if (CollUtil.isNotEmpty(tuples)) {
            redisTemplate.opsForZSet().add(redisKey, tuples);
            redisTemplate.expire(redisKey, interactionCacheTtlSeconds(), TimeUnit.SECONDS);
        }
    }

    private Long requireLoginUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        return userId;
    }

    private long zSetSize(String redisKey) {
        Long size = redisTemplate.opsForZSet().zCard(redisKey);
        return size == null ? 0L : size;
    }

    private long interactionCacheTtlSeconds() {
        return TimeUnit.DAYS.toSeconds(1) + RandomUtil.randomLong(TimeUnit.DAYS.toSeconds(1));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void noteCheckPass(Long noteId) {
        // 管理员可处理待审笔记，也可复核 AI 驳回的笔记；版本条件防止与编辑并发覆盖。
        //检查笔记当前的状态，只有状态在待审才可通过
        Note note = getById(noteId);
        if (note == null || !(Objects.equals(note.getStatus(), NoteStatusEnum.PENDING_REVIEW.getValue())
                || (Objects.equals(note.getStatus(), NoteStatusEnum.DOWNED.getValue()) && "AI".equals(note.getReviewMethod())))) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        //审核通过
        boolean result = lambdaUpdate()
                .set(Note::getStatus, NoteStatusEnum.NORMAL.getValue())
                .set(Note::getReviewMethod, "MANUAL")
                .set(Note::getReviewReason, null)
                .set(Note::getReviewedAt, LocalDateTime.now())
                .eq(Note::getId, noteId)
                .eq(Note::getStatus, note.getStatus())
                .eq(Note::getReviewVersion, note.getReviewVersion())
                .eq(Objects.equals(note.getStatus(), NoteStatusEnum.DOWNED.getValue()), Note::getReviewMethod, "AI")
                .update();
        if (result) {
            //审核通过发送笔记计数记录生成消息
            InsertNoteCountRecordDTO insertNoteCountRecordDTO = new InsertNoteCountRecordDTO(noteId);
            Message<String> message = MessageBuilder.withPayload(JsonUtil.toJsonString(insertNoteCountRecordDTO)).build();
            rocketMQTemplate.asyncSend(MQConstant.TOPIC_INSERT_NOTE_COUNT_RECORD, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("==============> 笔记审核通过初始化笔记计数记录消息发送成功, result: {}", sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("## 笔记审核通过初始化笔记计数记录消息发送成功, error: ", throwable);
                }
            });

            noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_UPSERT, LocalDateTime.now());
            noteAiReviewTaskService.markOverridden(noteId, note.getReviewVersion());
            evictNoteCache(noteId);
        } else {
            throw new BizException(ErrorCodeEnum.NOTE_CHECK_PASS_FAIL);
        }
    }

    @Override
    public MyNotesPageResponse queryMyNotes(QueryMyNotesRequest request) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        Integer status = request.getStatus();
        int limit = request.getLimit();

        // 1. 查询全部置顶笔记（不分页）
        List<Note> pinnedNotes = getBaseMapper().selectPinnedNotes(userId, status);

        // 2. 游标查询普通笔记（多查一条判断hasNext）
        List<Note> normalNotes = getBaseMapper().selectMyNotes(
                userId, status, request.getCursor(), limit + 1);

        boolean hasNext = false;
        Long nextCursor = null;
        if (CollUtil.isNotEmpty(normalNotes) && normalNotes.size() > limit) {
            hasNext = true;
            normalNotes = normalNotes.subList(0, limit);
            nextCursor = normalNotes.get(normalNotes.size() - 1).getId();
        }

        // 3. 合并所有笔记id批量查计数
        List<Long> pinnedIds = pinnedNotes.stream().map(Note::getId).toList();
        List<Long> normalIds = normalNotes.stream().map(Note::getId).toList();
        List<Long> allIds = new java.util.ArrayList<>();
        allIds.addAll(pinnedIds);
        allIds.addAll(normalIds);

        Map<Long, NoteCountResponse> countMap = null;
        if (CollUtil.isNotEmpty(allIds)) {
            List<NoteCountResponse> countList = countRpcService.batchQueryNoteCount(allIds);
            if (CollUtil.isNotEmpty(countList)) {
                countMap = countList.stream()
                        .collect(Collectors.toMap(NoteCountResponse::getNoteId, c -> c));
            }
        }

        Map<Long, NoteCountResponse> finalCountMap = countMap;

        // 4. 转换置顶笔记
        List<MyNoteItemResponse> pinnedResult = pinnedNotes.stream()
                .map(note -> buildItem(note, finalCountMap))
                .toList();

        // 5. 转换普通笔记
        List<MyNoteItemResponse> normalResult = normalNotes.stream()
                .map(note -> buildItem(note, finalCountMap))
                .toList();

        // 6. 总数
        Long total = lambdaQuery()
                .eq(Note::getCreatorId, userId)
                .ne(Note::getStatus, NoteStatusEnum.DELETED.getValue())
                .eq(status != null, Note::getStatus, status)
                .count();

        MyNotesPageResponse response = new MyNotesPageResponse();
        response.setPinnedNotes(pinnedResult);
        response.setNormalNotes(normalResult);
        response.setNextCursor(nextCursor);
        response.setHasNext(hasNext);
        response.setTotal(total);
        return response;
    }

    @Override
    public NoteAnalysisResponse analysis() {
        NoteAnalysisResponse response = new NoteAnalysisResponse();
        response.setLast7Days(buildAnalysisPeriod(7));
        response.setLast30Days(buildAnalysisPeriod(30));
        return response;
    }

    private NoteAnalysisPeriodResponse buildAnalysisPeriod(int days) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        List<NoteAnalysisNoteItemResponse> notes = getBaseMapper().selectAnalysisNotes(
                userId, startTime, endTime, NoteStatusEnum.NORMAL.getValue());
        if (notes == null) {
            notes = List.of();
        }

        NoteAnalysisPeriodResponse period = new NoteAnalysisPeriodResponse();
        period.setDays(days);
        period.setStartDate(startDate.toString());
        period.setEndDate(endDate.toString());
        period.setLikeTopNotes(topNotes(notes, NoteAnalysisNoteItemResponse::getLikeCount, 10));
        period.setCollectTopNotes(topNotes(notes, NoteAnalysisNoteItemResponse::getCollectCount, 10));
        period.setCommentTopNotes(topNotes(notes, NoteAnalysisNoteItemResponse::getCommentCount, 5));
        period.setChannelWordCloud(buildChannelWordCloud(notes));
        return period;
    }

    private List<NoteAnalysisNoteItemResponse> topNotes(List<NoteAnalysisNoteItemResponse> notes,
                                                        ToLongFunction<NoteAnalysisNoteItemResponse> countGetter,
                                                        int limit) {
        return notes.stream()
                .sorted(Comparator.comparingLong(countGetter)
                        .reversed()
                        .thenComparing(NoteAnalysisNoteItemResponse::getCreateTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NoteAnalysisNoteItemResponse::getNoteId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private List<ChannelWordCloudItemResponse> buildChannelWordCloud(List<NoteAnalysisNoteItemResponse> notes) {
        Map<String, Long> channelCountMap = notes.stream()
                .map(NoteAnalysisNoteItemResponse::getChannelName)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.groupingBy(channelName -> channelName, Collectors.counting()));
        return channelCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> {
                    ChannelWordCloudItemResponse item = new ChannelWordCloudItemResponse();
                    item.setWord(entry.getKey());
                    item.setCount(entry.getValue());
                    item.setWeight(entry.getValue());
                    return item;
                })
                .toList();
    }

    @Override
    public CursorPageResponse<AdminNoteItemResponse> adminNoteList(AdminNoteListRequest request) {
        if (request == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        int limit = request.getLimit();
        if (limit <= 0) {
            limit = 20;
        }
        if (limit > 50) {
            limit = 50;
        }
        //多查一条判断是否还有下一页
        List<Note> notes = getBaseMapper().selectAdminNotes(
                request.getStatus(), request.getCreatorId(), request.getCursor(), limit + 1);
        if (CollUtil.isEmpty(notes)) {
            return CursorPageResponse.success(List.of(), null, false, 0, 0L);
        }
        boolean hasNext = notes.size() > limit;
        Long nextCursor = null;
        if (hasNext) {
            notes = notes.subList(0, limit);
            nextCursor = notes.get(notes.size() - 1).getId();
        }
        //批量查询笔记计数
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        Map<Long, NoteCountResponse> countMap = null;
        List<NoteCountResponse> countList = countRpcService.batchQueryNoteCount(noteIds);
        if (CollUtil.isNotEmpty(countList)) {
            countMap = countList.stream().collect(Collectors.toMap(NoteCountResponse::getNoteId, c -> c));
        }
        //批量查询创作者信息
        List<Long> creatorIds = notes.stream().map(Note::getCreatorId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, FindUserResponse> userMap = batchQueryUserMap(creatorIds);

        Map<Long, NoteCountResponse> finalCountMap = countMap;
        List<AdminNoteItemResponse> items = notes.stream()
                .map(note -> buildAdminItem(note, finalCountMap, userMap))
                .toList();

        Long total = lambdaQuery()
                .eq(request.getStatus() != null, Note::getStatus, request.getStatus())
                .eq(request.getCreatorId() != null, Note::getCreatorId, request.getCreatorId())
                .count();
        return CursorPageResponse.success(items, nextCursor, hasNext, items.size(), total);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminCheckReject(Long noteId, String reason) {
        // 人工驳回必须填写作者可读的原因，也允许管理员复核并驳回 AI 已通过的笔记。
        reason = StrUtil.trim(reason);
        if (StrUtil.isBlank(reason) || reason.length() > 200) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        //只有待审核状态才能驳回
        Note note = getById(noteId);
        if (note == null || !(Objects.equals(note.getStatus(), NoteStatusEnum.PENDING_REVIEW.getValue())
                || (Objects.equals(note.getStatus(), NoteStatusEnum.NORMAL.getValue()) && "AI".equals(note.getReviewMethod())))) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        boolean result = lambdaUpdate()
                .set(Note::getStatus, NoteStatusEnum.DOWNED.getValue())
                .set(Note::getReviewMethod, "MANUAL")
                .set(Note::getReviewReason, reason)
                .set(Note::getReviewedAt, LocalDateTime.now())
                .set(Note::getUpdateTime, LocalDateTime.now())
                .eq(Note::getId, noteId)
                .eq(Note::getStatus, note.getStatus())
                .eq(Note::getReviewVersion, note.getReviewVersion())
                .eq(Objects.equals(note.getStatus(), NoteStatusEnum.NORMAL.getValue()), Note::getReviewMethod, "AI")
                .update();
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_CHECK_REJECT_FAIL);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, LocalDateTime.now());
        noteAiReviewTaskService.markOverridden(noteId, note.getReviewVersion());
        evictNoteCache(noteId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDownNote(AdminDownNoteRequest request) {
        if (request == null || request.getNoteId() == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        AdminDownReasonEnum reasonType = AdminDownReasonEnum.fromCode(request.getReasonCode());
        if (reasonType == null) {
            throw new BizException(ErrorCodeEnum.NOTE_DOWN_REASON_INVALID);
        }
        String reasonDetail = StrUtil.trim(request.getReasonDetail());
        if (reasonDetail != null && reasonDetail.length() > 200) {
            throw new BizException(ErrorCodeEnum.NOTE_DOWN_REASON_DETAIL_TOO_LONG);
        }
        if (reasonType == AdminDownReasonEnum.OTHER && StrUtil.isBlank(reasonDetail)) {
            throw new BizException(ErrorCodeEnum.NOTE_DOWN_REASON_DETAIL_REQUIRED);
        }
        String reason = StrUtil.isBlank(reasonDetail)
                ? reasonType.getLabel()
                : reasonType.getLabel() + "：" + reasonDetail;
        Long noteId = request.getNoteId();
        //只有正常展示状态才能下架
        Note note = lambdaQuery().eq(Note::getId, noteId)
                .eq(Note::getStatus, NoteStatusEnum.NORMAL.getValue()).one();
        if (Objects.isNull(note)) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        boolean result = lambdaUpdate()
                .set(Note::getStatus, NoteStatusEnum.DOWNED.getValue())
                .set(Note::getUpdateTime, LocalDateTime.now())
                .eq(Note::getId, noteId)
                .eq(Note::getStatus, NoteStatusEnum.NORMAL.getValue())
                .update();
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_ADMIN_DOWN_FAIL);
        }
        LocalDateTime occurredAt = LocalDateTime.now();
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, occurredAt);
        noteNotificationOutboxService.appendDowned(note, reasonType.name(), reason, occurredAt);
        evictNoteCache(noteId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminRestoreNote(Long noteId) {
        //只有下架/未通过状态才能恢复上架
        Note note = lambdaQuery().eq(Note::getId, noteId)
                .eq(Note::getStatus, NoteStatusEnum.DOWNED.getValue()).one();
        if (Objects.isNull(note)) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        boolean result = lambdaUpdate()
                .set(Note::getStatus, NoteStatusEnum.NORMAL.getValue())
                .set(Note::getReviewMethod, "MANUAL")
                .set(Note::getReviewReason, null)
                .set(Note::getReviewedAt, LocalDateTime.now())
                .set(Note::getUpdateTime, LocalDateTime.now())
                .eq(Note::getId, noteId)
                .eq(Note::getStatus, NoteStatusEnum.DOWNED.getValue())
                .eq(Note::getReviewVersion, note.getReviewVersion())
                .update();
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_ADMIN_RESTORE_FAIL);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_UPSERT, LocalDateTime.now());
        noteAiReviewTaskService.markOverridden(noteId, note.getReviewVersion());
        evictNoteCache(noteId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDeleteNote(Long noteId) {
        Note note = getById(noteId);
        if (Objects.isNull(note)) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        if (Objects.equals(note.getStatus(), NoteStatusEnum.DELETED.getValue())) {
            throw new BizException(ErrorCodeEnum.NOTE_STATUS_ERROR_OR_EXISTS);
        }
        Note updateNote = new Note();
        updateNote.setId(noteId);
        updateNote.setStatus(NoteStatusEnum.DELETED.getValue());
        updateNote.setUpdateTime(LocalDateTime.now());
        boolean result = updateById(updateNote);
        if (!result) {
            throw new BizException(ErrorCodeEnum.NOTE_ADMIN_DELETE_FAIL);
        }
        noteRagOutboxService.append(noteId, NoteRagIndexMessage.EVENT_DELETE, updateNote.getUpdateTime());
        evictNoteCache(noteId);
        //发送删除计数消息(使用原作者的id，减少其发布笔记计数)
        String destination = MQConstant.TOPIC_COUNT_NOTE_PUBLISH + ":" + MQConstant.TAG_DELETE;
        MQPublishCountDTO mqPublishCountDTO = new MQPublishCountDTO(note.getCreatorId(), NotePublishEnum.DELETE.getCode(), LocalDateTime.now());
        Message<MQPublishCountDTO> message = MessageBuilder.withPayload(mqPublishCountDTO).build();
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("=======> 管理员删除笔记计数消息发送成功, result: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("## 管理员删除笔记计数消息发送失败, error: ", throwable);
            }
        });
    }

    private AdminNoteItemResponse buildAdminItem(Note note, Map<Long, NoteCountResponse> countMap,
                                                 Map<Long, FindUserResponse> userMap) {
        AdminNoteItemResponse item = new AdminNoteItemResponse();
        item.setId(note.getId());
        item.setTitle(note.getTitle());
        item.setType(note.getType());
        item.setStatus(note.getStatus());
        item.setVisible(note.getVisible());
        item.setIsTop(note.getIsTop());
        item.setCreatorId(note.getCreatorId());
        item.setTopicId(note.getTopicId());
        item.setTopicName(note.getTopicName());
        item.setCreateTime(note.getCreateTime());
        item.setUpdateTime(note.getUpdateTime());
        item.setReviewMethod(note.getReviewMethod());
        item.setReviewReason(note.getReviewReason());
        item.setReviewedAt(note.getReviewedAt());
        item.setReviewVersion(note.getReviewVersion());
        Map<String, Object> reviewTask = noteAiReviewTaskService.latest(note.getId(), note.getReviewVersion());
        item.setReviewTaskStatus((String) reviewTask.get("status"));
        item.setReviewInternalError((String) reviewTask.get("internal_error"));
        if (StrUtil.isNotBlank(note.getImgUris())) {
            item.setCover(note.getImgUris().split(",")[0]);
        } else if (Objects.equals(note.getType(), NoteTypeEnum.VIDEO.getValue())
                && StrUtil.isNotBlank(note.getVideoUri())) {
            item.setCover(note.getVideoUri());
        }
        if (Objects.nonNull(userMap) && userMap.containsKey(note.getCreatorId())) {
            item.setCreatorName(userMap.get(note.getCreatorId()).getNickname());
        }
        if (Objects.nonNull(countMap) && countMap.containsKey(note.getId())) {
            NoteCountResponse c = countMap.get(note.getId());
            item.setLikeCount(c.getLikeTotal());
            item.setCollectCount(c.getCollectTotal());
            item.setCommentCount(c.getCommentTotal());
        }
        return item;
    }

    private Map<Long, FindUserResponse> batchQueryUserMap(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<FindUserResponse> users = userRpcService.batchQueryUser(userIds);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(FindUserResponse::getId, u -> u));
    }

    private void evictNoteCache(Long noteId) {
        String redisKey = RedisKeyConstant.NOTE_DETAIL_KEY_PREFIX + noteId;
        redisTemplate.delete(redisKey);
        CaffeineUtil.deleteNoteDetail(noteId);
        try {
            rocketMQTemplate.syncSend(MQConstant.TOPIC_DELETE_NOTE_LOCAL_CACHE, noteId);
            log.info("====> MQ：删除笔记本地缓存发送成功...");
        } catch (Exception e) {
            // 缓存广播失败不能回滚笔记状态和事务 Outbox；远端实例缓存最终会自然过期。
            log.warn("笔记本地缓存广播失败, noteId={}", noteId, e);
        }
    }

    private MyNoteItemResponse buildItem(Note note, Map<Long, NoteCountResponse> countMap) {
        MyNoteItemResponse item = new MyNoteItemResponse();
        item.setId(note.getId());
        item.setTitle(note.getTitle());
        if (StrUtil.isNotBlank(note.getImgUris())) {
            item.setCover(note.getImgUris().split(",")[0]);
        } else if (note.getType() != null && note.getType() == 1
                && StrUtil.isNotBlank(note.getVideoUri())) {
            item.setCover(note.getVideoUri());
        }
        item.setCreateTime(note.getCreateTime());
        item.setIsTop(note.getIsTop());
        item.setType(note.getType());
        item.setStatus(note.getStatus());
        item.setReviewReason(note.getReviewReason());
        if (countMap != null && countMap.containsKey(note.getId())) {
            NoteCountResponse c = countMap.get(note.getId());
            item.setCommentCount(c.getCommentTotal());
            item.setLikeCount(c.getLikeTotal());
            item.setCollectCount(c.getCollectTotal());
        }
        return item;
    }

    private boolean canAccessNoteItem(Note note, Long loginUserId) {
        if (note == null || Objects.equals(note.getStatus(), NoteStatusEnum.DELETED.getValue())) {
            return false;
        }
        if (Objects.equals(note.getCreatorId(), loginUserId)) {
            return true;
        }
        return Objects.equals(note.getStatus(), NoteStatusEnum.NORMAL.getValue())
                && Objects.equals(note.getVisible(), NoteVisibleEnum.PUBLIC.getValue());
    }

    private Map<Long, NoteCountResponse> batchQueryNoteCountMap(List<Long> noteIds) {
        if (CollUtil.isEmpty(noteIds)) {
            return Map.of();
        }
        List<NoteCountResponse> noteCountResponses = countRpcService.batchQueryNoteCount(noteIds);
        if (CollUtil.isEmpty(noteCountResponses)) {
            return Map.of();
        }
        return noteCountResponses.stream()
                .filter(Objects::nonNull)
                .filter(response -> response.getNoteId() != null)
                .collect(Collectors.toMap(NoteCountResponse::getNoteId, response -> response, (left, right) -> left));
    }

    private Set<Long> queryLikedNoteIds(Long loginUserId, List<Long> noteIds) {
        if (loginUserId == null || CollUtil.isEmpty(noteIds)) {
            return Set.of();
        }
        return noteLikeService.lambdaQuery()
                .select(NoteLike::getNoteId)
                .eq(NoteLike::getUserId, loginUserId)
                .eq(NoteLike::getStatus, NoteLikeEnum.LIKE.getStatus())
                .in(NoteLike::getNoteId, noteIds)
                .list()
                .stream()
                .map(NoteLike::getNoteId)
                .collect(Collectors.toSet());
    }

    private Set<Long> queryCollectedNoteIds(Long loginUserId, List<Long> noteIds) {
        if (loginUserId == null || CollUtil.isEmpty(noteIds)) {
            return Set.of();
        }
        return noteCollectionService.lambdaQuery()
                .select(NoteCollection::getNoteId)
                .eq(NoteCollection::getUserId, loginUserId)
                .eq(NoteCollection::getStatus, NoteCollectStatus.COLLECT.getCode())
                .in(NoteCollection::getNoteId, noteIds)
                .list()
                .stream()
                .map(NoteCollection::getNoteId)
                .collect(Collectors.toSet());
    }

    private Map<Long, FollowResultResponse> queryFollowMap(Long loginUserId, List<Long> creatorIds) {
        if (loginUserId == null || CollUtil.isEmpty(creatorIds)) {
            return Map.of();
        }
        List<FollowResultResponse> resultResponses = userRelationRpcService.checkFollow(loginUserId, creatorIds);
        if (CollUtil.isEmpty(resultResponses)) {
            return Map.of();
        }
        return resultResponses.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(FollowResultResponse::getCreatorId, result -> result, (left, right) -> left));
    }

    /**
     * 校验笔记是否可见
     * @param userId 当前登陆用户
     * @param creatorId 创作者id
     * @param visible 可见标识
     */
    private void checkNoteVisible(Long userId, Long creatorId, Integer visible) {
        if (!creatorId.equals(userId) &&
                visible.equals(NoteVisibleEnum.PRIVATE.getValue())) {
            throw new BizException(ErrorCodeEnum.NOTE_PRIVATE);
        }
    }

    /**
     * 非作者只能访问正常状态的笔记；作者仍可查看自己的已下架笔记。
     */
    private void checkNoteAccessible(Long userId, Long creatorId, Integer status, Integer visible) {
        if (!Objects.equals(creatorId, userId)
                && !Objects.equals(status, NoteStatusEnum.NORMAL.getValue())) {
            throw new BizException(ErrorCodeEnum.NOTE_NOT_FOUND);
        }
        checkNoteVisible(userId, creatorId, visible);
    }
}
