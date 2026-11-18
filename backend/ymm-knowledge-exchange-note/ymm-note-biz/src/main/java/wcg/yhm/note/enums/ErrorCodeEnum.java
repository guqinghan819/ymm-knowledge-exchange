package wcg.yhm.note.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 错误码枚举
 * @Version: 1.0
 */

@Getter
public enum ErrorCodeEnum implements BaseException {
    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("NOTE-10000", "笔记服务异常"),
    PARAM_NOT_VALID("NOTE-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    NOTE_TYPE_ERROR("NOTE-20000", "未知的笔记类型"),
    NOTE_PUBLISH_FAIL("NOTE-20001", "笔记发布失败"),
    NOTE_IMAGE_COUNT_ERROR("NOTE-20002", "图片上传不能多于8张"),
    NOTE_VIDEO_URL_ERROR("NOTE-20003", "视频地址空异常"),
    NOTE_NOT_FOUND("NOTE-20004", "笔记不存在"),
    NOTE_PRIVATE("NOTE-20005", "作者已将该笔记设置为仅自己可见"),
    NOTE_UPDATE_FAIL("NOTE-20006", "笔记更新失败"),
    NOTE_SET_VISIBLE_FAIL("NOTE-20007", "笔记设置私密失败"),
    NOTE_SET_TOP_NO_AUTH("NOTE-20008", "没有权限设置笔记置顶"),
    NOTE_NOT_AUTH("NOTE-20009", "没有权限"),
    NOTE_ALREADY_LIKE("NOTE-20010", "笔记已经点赞"),
    UNLIKE_OPERATION_FAIL("NOTE_20011", "该笔记没有点赞, 不能进行取消点赞操作"),
    NOTE_ALREADY_COLLECT("NOTE-20012", "笔记已经收藏"),
    UN_COLLECT_OPERATION_FAIL("NOTE_20013", "该笔记没有收藏, 不能进行取消收藏操作"),
    CHANNEL_NO_EXISTS("NOTE-20014", "该频道不存在"),
    NOTE_STATUS_ERROR_OR_EXISTS("NOTE-20015", "笔记状态错误或笔记不存在"),
    NOTE_CHECK_PASS_FAIL("NOTE-20016", "笔记审核通过失败"),
    NOTE_CHECK_REJECT_FAIL("NOTE-20017", "笔记审核驳回失败"),
    NOTE_ADMIN_DOWN_FAIL("NOTE-20018", "笔记下架失败"),
    NOTE_ADMIN_RESTORE_FAIL("NOTE-20019", "笔记恢复上架失败"),
    NOTE_ADMIN_DELETE_FAIL("NOTE-20020", "笔记删除失败"),
    CHANNEL_NAME_EXISTS("NOTE-20021", "频道名称已存在"),
    CHANNEL_CREATE_FAIL("NOTE-20022", "频道创建失败"),
    CHANNEL_UPDATE_FAIL("NOTE-20023", "频道更新失败"),
    CHANNEL_DELETE_FAIL("NOTE-20024", "频道删除失败"),
    TOPIC_NAME_EXISTS("NOTE-20025", "话题名称已存在"),
    TOPIC_CREATE_FAIL("NOTE-20026", "话题创建失败"),
    TOPIC_UPDATE_FAIL("NOTE-20027", "话题更新失败"),
    TOPIC_DELETE_FAIL("NOTE-20028", "话题删除失败"),
    TOPIC_HAS_NOTES("NOTE-20029", "该话题下存在笔记，请先处理相关笔记"),
    CHANNEL_TOPIC_BIND_FAIL("NOTE-20030", "频道话题绑定失败"),
    TOPIC_NOT_FOUND("NOTE-20031", "话题不存在"),
    NOTE_DOWN_REASON_INVALID("NOTE-20032", "下架原因无效"),
    NOTE_DOWN_REASON_DETAIL_REQUIRED("NOTE-20033", "选择其他原因时必须填写补充说明"),
    NOTE_DOWN_REASON_DETAIL_TOO_LONG("NOTE-20034", "下架原因补充说明最多200个字符"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
