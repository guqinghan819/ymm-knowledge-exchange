package wcg.yhm.user.relation.biz.enums;


import lombok.Getter;
import wcg.yhm.ymmknowledgeexchange.common.exception.BaseException;

@Getter
public enum ErrorCodeEnum implements BaseException {
    SYSTEM_ERROR("USER-RELATION-10000", "用户关注服务异常"),
    PARAM_NOT_VALID("USER-RELATION-10001", "参数错误"),
    CANT_FOLLOW_YOUR_SELF("USER-RELATION-20001", "无法关注自己"),
    FOLLOW_USER_NOT_EXISTED("USER-RELATION-20002", "关注的用户不存在"),
    FOLLOWING_COUNT_LIMIT("USER-RELATION-20003", "您关注的用户已达上限，请先取关部分用户"),
    ALREADY_FOLLOWED("USER-RELATION-20004", "您已经关注了该用户"),
    CANT_UNFOLLOW_YOUR_SELF("USER-RELATION-20005", "无法取关自己"),
    NOT_FOLLOWED("USER-RELATION-20006", "你未关注对方，无法取关"),
    ERROR_PAGE_NO("USER-RELATION-20007", "错误查询页码"),
    ;
    private final String errorCode;
    private final String errorMessage;

    ErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
