package wcg.yhm.text.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;
import wcg.yhm.text.api.domain.dto.*;
import wcg.yhm.text.biz.domain.po.CommentContent;
import wcg.yhm.text.biz.domain.po.CommentContentPrimaryKey;
import wcg.yhm.text.biz.domain.po.NoteContent;
import wcg.yhm.text.biz.enums.ErrorCodeEnum;
import wcg.yhm.text.biz.repository.CommentContentRepository;
import wcg.yhm.text.biz.repository.TextRepository;
import wcg.yhm.text.biz.service.TextService;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: 实现类
 * @Version: 1.0
 */

@Service
@Slf4j
public class TextServiceImpl implements TextService {
    @Resource
    private TextRepository textRepository;

    @Resource
    private CassandraTemplate cassandraTemplate;

    @Resource
    private CommentContentRepository commentContentRepository;

    @Override
    public void addText(AddNoteContentRequest addNoteContentRequest) {
        //1.校验参数
        if (addNoteContentRequest == null) {
            throw new BizException(ErrorCodeEnum.PARAM_NOT_VALID);
        }
        String id = addNoteContentRequest.getId();
        String content = addNoteContentRequest.getContent();
        //添加
        NoteContent noteContent = new NoteContent();
        noteContent.setContent(content);
        noteContent.setId(UUID.fromString(id));
        try {
            textRepository.save(noteContent);
        } catch (Exception e) {
            throw new BizException(ErrorCodeEnum.OPERATION_FAIL);
        }
    }

    @Override
    public QueryByIdResponse queryById(QueryByIdRequest queryByIdRequest) {
        String id = queryByIdRequest.getId();
        Optional<NoteContent> optional = textRepository.findById(UUID.fromString(id));
        if (optional.isEmpty()) {
            throw new BizException(ErrorCodeEnum.CONTENT_NOT_EXIST);
        }
        NoteContent noteContent = optional.get();
        QueryByIdResponse queryByIdResponse = new QueryByIdResponse();
        queryByIdResponse.setId(noteContent.getId());
        queryByIdResponse.setContent(noteContent.getContent());
        return queryByIdResponse;
    }

    @Override
    public void delete(DeleteRequest deleteRequest) {
        String id = deleteRequest.getId();
        try {
            textRepository.deleteById(UUID.fromString(id));
        } catch (Exception e) {
            throw new BizException(ErrorCodeEnum.OPERATION_FAIL);
        }
    }

    @Override
    public void batchAddComment(BatchAddCommentRequest batchAddCommentRequest) {
        // 评论正文使用笔记 ID、发布年月和内容 UUID 组成 Cassandra 主键。
        List<CommentContentDTO> contentDTOList = batchAddCommentRequest.getContentDTOList();

        List<CommentContent> contents = contentDTOList.stream().map(commentContentDTO -> {
            CommentContent commentContent = new CommentContent();
            commentContent.setContent(commentContentDTO.getContent());
            CommentContentPrimaryKey primaryKey = CommentContentPrimaryKey.builder()
                    .noteId(commentContentDTO.getNoteId())
                    .contentId(UUID.fromString(commentContentDTO.getContentId()))
                    .yearMonth(commentContentDTO.getYearMonth())
                    .build();
            commentContent.setPrimaryKey(primaryKey);
            return commentContent;
        }).toList();
        //批量插入
        cassandraTemplate.batchOps().insert(contents).execute();
    }



    @Override
    public List<FindCommentContentResponse> batchFindCommentContent(BatchFindCommentContentRequest batchFindCommentContentRequest) {
        // 按同一笔记的年月和内容 UUID 批量查询，减少逐条读取 Cassandra 的开销。


        // 归属的笔记 ID
        Long noteId = batchFindCommentContentRequest.getNoteId();

        // 查询评论的发布年月、内容 UUID
        List<FindCommentContentDTO> commentContentKeys = batchFindCommentContentRequest.getCommentContentKeys();

        // 过滤出年月
        List<String> yearMonths = commentContentKeys.stream()
                .map(FindCommentContentDTO::getYearMonth)
                .distinct() // 去重
                .collect(Collectors.toList());

        // 过滤出评论内容 UUID
        List<UUID> contentIds = commentContentKeys.stream()
                .map(commentContentKey -> UUID.fromString(commentContentKey.getContentId()))
                .distinct() // 去重
                .collect(Collectors.toList());

        // 批量查询 Cassandra
        List<CommentContent> commentContents = commentContentRepository
                .findByPrimaryKeyNoteIdAndPrimaryKeyYearMonthInAndPrimaryKeyContentIdIn(noteId, yearMonths, contentIds);

        // DO 转 DTO
        List<FindCommentContentResponse> findCommentContentResponseList = new ArrayList<>();
        if (CollUtil.isNotEmpty(commentContents)) {
            findCommentContentResponseList = commentContents.stream()
                    .map(commentContent -> FindCommentContentResponse.builder()
                            .contentId(String.valueOf(commentContent.getPrimaryKey().getContentId()))
                            .content(commentContent.getContent())
                            .build())
                    .toList();
        }

        return findCommentContentResponseList;
    }
}
    
