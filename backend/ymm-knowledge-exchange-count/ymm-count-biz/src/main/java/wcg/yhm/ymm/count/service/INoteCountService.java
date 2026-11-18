package wcg.yhm.ymm.count.service;


import com.baomidou.mybatisplus.extension.service.IService;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymm.count.domain.po.NoteCount;

import java.util.List;

/**
 * <p>
 * 笔记计数表 服务类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
public interface INoteCountService extends IService<NoteCount> {

    /**
     * 批量查询笔记计数
     * @param noteIds 笔记id列表
     * @return
     */
    List<NoteCountResponse> batchQueryByIds(List<Long> noteIds);
}
