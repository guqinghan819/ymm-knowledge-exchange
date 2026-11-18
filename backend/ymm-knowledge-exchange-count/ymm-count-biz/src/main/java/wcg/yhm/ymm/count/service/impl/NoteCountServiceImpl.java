package wcg.yhm.ymm.count.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymm.count.domain.po.NoteCount;
import wcg.yhm.ymm.count.mapper.NoteCountMapper;
import wcg.yhm.ymm.count.service.INoteCountService;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 笔记计数表 服务实现类
 * </p>
 *
 * @author 雨好绵
 * @since 2026-02-03
 */
@Service
public class NoteCountServiceImpl extends ServiceImpl<NoteCountMapper, NoteCount> implements INoteCountService {

    @Override
    public List<NoteCountResponse> batchQueryByIds(List<Long> noteIds) {
        List<NoteCount> noteCountList = lambdaQuery().in(NoteCount::getNoteId, noteIds).list();
        List<NoteCountResponse> noteCountResponses = new ArrayList<>();
        noteCountList.forEach(noteCount -> {
            noteCountResponses.add(BeanUtil.copyProperties(noteCount, NoteCountResponse.class));
        });
        return noteCountResponses;
    }
}
