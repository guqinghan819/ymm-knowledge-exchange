package wcg.yhm.note.rpc;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import wcg.yhm.text.api.controller.TextFeignApi;
import wcg.yhm.text.api.domain.dto.AddNoteContentRequest;
import wcg.yhm.text.api.domain.dto.DeleteRequest;
import wcg.yhm.text.api.domain.dto.QueryByIdRequest;
import wcg.yhm.text.api.domain.dto.QueryByIdResponse;
import wcg.yhm.ymmknowledgeexchange.common.response.Response;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 短文本存储调用服务
 * @Version: 1.0
 */

@Component
public class TextRpcService {

    @Resource
    private TextFeignApi textFeignApi;

    /**
     * 保存笔记文本
     * @param uuid
     * @param content
     * @return
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentRequest addNoteContentRequest = new AddNoteContentRequest();
        addNoteContentRequest.setId(uuid);
        addNoteContentRequest.setContent(content);

        Response<?> response = textFeignApi.addContent(addNoteContentRequest);
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            return false;
        }
        return true;
    }


    /**
     * 删除笔记内容
     * @param uuid
     * @return
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteRequest deleteRequest = new DeleteRequest();
        deleteRequest.setId(uuid);

        Response<?> response = textFeignApi.delete(deleteRequest);
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            return false;
        }
        return true;
    }

    /**
     * 查询笔记内容
     * @param uuid id
     * @return 笔记内容
     */
    public String getContent(String uuid) {
        QueryByIdRequest queryByIdRequest = new QueryByIdRequest();
        queryByIdRequest.setId(uuid);
        Response<QueryByIdResponse> resp = textFeignApi.findContentById(queryByIdRequest);
        if (ObjectUtil.isNull(resp) || !resp.isSuccess() || ObjectUtil.isNull(resp.getData())) {
            return null;
        }
        return resp.getData().getContent();
    }
}
    
