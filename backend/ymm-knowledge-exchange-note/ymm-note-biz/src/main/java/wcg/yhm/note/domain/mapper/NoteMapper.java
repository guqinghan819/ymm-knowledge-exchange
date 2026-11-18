package wcg.yhm.note.domain.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import wcg.yhm.note.domain.dataobject.Note;
import wcg.yhm.note.model.vo.NoteAnalysisNoteItemResponse;
import wcg.yhm.note.model.vo.AdminChannelWordCloudResponse;
import wcg.yhm.note.model.vo.AdminLikedNoteResponse;
import wcg.yhm.note.model.vo.AdminUserRankResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 笔记表 Mapper 接口
 * </p>
 *
 * @author 雨好绵
 * @since 2025-12-23
 */
public interface NoteMapper extends BaseMapper<Note> {
    /**
     * 游标分页（获取下一页）
     * @param cursor 上一页最后一条记录id
     * @param limit 每页数量
     * @return 笔记列表
     */
    List<Note> selectByCursor(@Param("cursor") Long cursor,
                              @Param("limit") int limit,
                              @Param("topicIds") List<Long> topicIds,
                              @Param("loginUserId") Long loginUserId);

    List<Note> selectMyNotes(@Param("creatorId") Long creatorId,
                             @Param("status") Integer status,
                             @Param("cursor") Long cursor,
                             @Param("limit") int limit);

    List<Note> selectPinnedNotes(@Param("creatorId") Long creatorId,
                                 @Param("status") Integer status);

    /**
     * 管理端游标分页查询笔记列表
     * @param status 状态(可选)
     * @param creatorId 创作者id(可选)
     * @param cursor 上一页最后一条记录id
     * @param limit 每页数量
     * @return 笔记列表
     */
    List<Note> selectAdminNotes(@Param("status") Integer status,
                                @Param("creatorId") Long creatorId,
                                @Param("cursor") Long cursor,
                                @Param("limit") int limit);

    List<NoteAnalysisNoteItemResponse> selectAnalysisNotes(@Param("creatorId") Long creatorId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime,
                                                           @Param("status") Integer status);

    List<AdminUserRankResponse> selectTopPublishers(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime,
                                                    @Param("deletedStatus") Integer deletedStatus,
                                                    @Param("limit") int limit);

    List<AdminLikedNoteResponse> selectTopLikedNotes(@Param("normalStatus") Integer normalStatus,
                                                     @Param("publicVisible") Integer publicVisible,
                                                     @Param("limit") int limit);

    List<AdminChannelWordCloudResponse> selectAdminChannelWordCloud(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("deletedStatus") Integer deletedStatus);
}
