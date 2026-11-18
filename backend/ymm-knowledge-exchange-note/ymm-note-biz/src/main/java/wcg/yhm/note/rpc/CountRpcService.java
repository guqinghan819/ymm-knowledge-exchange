package wcg.yhm.note.rpc;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.count.api.controller.CountFeignApi;
import wcg.yhm.ymm.count.api.domain.request.BatchQueryNoteCountRequest;
import wcg.yhm.ymm.count.api.domain.response.NoteCountResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

import java.util.List;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-23
 * @Description: 计数服务远程调用
 * @Version: 1.0
 */

@Component
@Slf4j
public class CountRpcService {
    @Resource
    private CountFeignApi countFeignApi;

    /**
     * 批量查询笔记计数
     * @return 计数列表
     */
    public List<NoteCountResponse> batchQueryNoteCount(List<Long> noteIds) {
        BatchQueryNoteCountRequest batchQueryNoteCountRequest = new BatchQueryNoteCountRequest();
        batchQueryNoteCountRequest.setNoteIds(noteIds);
        Response<List<NoteCountResponse>> response = countFeignApi.batchQueryNoteCount(batchQueryNoteCountRequest);

        if (response == null || !response.isSuccess()) {
            return null;
        }
        return response.getData();
    }
}
    
