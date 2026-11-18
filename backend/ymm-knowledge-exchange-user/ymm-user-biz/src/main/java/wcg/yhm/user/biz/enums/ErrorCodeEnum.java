package wcg.yhm.user.biz.enums;

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
    SYSTEM_ERROR("USER-10000", "用户服务系统异常 ( ´•̥×•̥` ) "),
    PARAM_NOT_VALID("USER-10001", "参数错误, Ciallo～(∠・ω< )⌒★"),
    NICK_NAME_LENGTH_ERROR("USER-20001", "昵称长度不合法(2-8)"),
    PASSWORD_ERROR("USER-20002", "密码异常(8-16), 仅可由数字、字母、. 组成"),
    INTRODUCE_ERROR("USER-20003", "简介长度异常(1-150)"),
    AVATAR_UPLOAD_FAIL("USER-20005", "头像上传失败"),
    BACKGROUND_IMAGE_UPLOAD_FAIL("USER-20006", "背景图上传失败"),
    USER_NOT_FOUND("USER-20006", "该用户不存在 Σヽ(ﾟД ﾟ; )ﾉ"),
    USER_ALREADY_EXISTS("USER-20021", "该手机号已注册，请使用原密码登录"),
    OPERATION_ERROR("USER-20004", "操作失败"),
    ROLE_NAME_EXISTS("USER-20010", "角色名已存在"),
    ROLE_KEY_EXISTS("USER-20011", "角色标识已存在"),
    ROLE_NOT_FOUND("USER-20012", "角色不存在"),
    ROLE_CREATE_FAIL("USER-20013", "角色创建失败"),
    ROLE_UPDATE_FAIL("USER-20014", "角色更新失败"),
    ROLE_DELETE_FAIL("USER-20015", "角色删除失败"),
    ROLE_HAS_USERS("USER-20016", "该角色下存在用户，无法删除"),
    ROLE_NOT_AVAILABLE("USER-20017", "角色不存在或已被禁用"),
    PERMISSION_NOT_AVAILABLE("USER-20018", "权限不存在或已被禁用"),
    RBAC_RELATION_UPDATE_FAIL("USER-20019", "角色权限关系更新失败"),
    ADMIN_ROLE_PROTECTED("USER-20020", "admin为系统保留角色，不能修改标识、禁用或删除");
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
