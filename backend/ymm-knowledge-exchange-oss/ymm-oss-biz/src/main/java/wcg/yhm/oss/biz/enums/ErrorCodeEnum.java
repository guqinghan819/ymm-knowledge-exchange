package wcg.yhm.oss.biz.enums;

import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description: 错误码枚举
 * @Version: 1.0
 */

@Getter
public enum ErrorCodeEnum implements BaseException {
    EMPTY_FILE_ERROR("OSS-10001", "文件为空 (´･ω･`) "),
    FILE_NAME_ERROR("OSS-10002", "文件名异常，请详细检查文件扩展名 Σ(ﾟДﾟ；≡；ﾟдﾟ) "),
    OSS_SYSTEM_ERROR("OSS-10000", "存储服务系统异常 （¯﹃¯） "),
    FILE_UPLOAD_FAIL("OSS-20001", "文件上传异常 (#`Д´)ﾉ "),
    CREATE_TEMP_FILE_FAIL("OSS-20002", "创建临时文件失败 (#`Д´)ﾉ "),
    VIDEO_MULTIPART_PARAM_ERROR("OSS-21001", "视频分片上传参数不合法"),
    VIDEO_MULTIPART_TASK_EXPIRED("OSS-21002", "视频上传任务不存在或已过期"),
    VIDEO_MULTIPART_FORBIDDEN("OSS-21003", "无权操作该视频上传任务"),
    VIDEO_MULTIPART_PART_ERROR("OSS-21004", "视频分片编号不合法"),
    VIDEO_MULTIPART_OPERATION_FAIL("OSS-21005", "COS视频分片操作失败"),
    VIDEO_MULTIPART_INCOMPLETE("OSS-21006", "视频分片尚未全部上传"),
    VIDEO_MULTIPART_STATE_ERROR("OSS-21007", "视频上传任务状态不允许当前操作");
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
    
