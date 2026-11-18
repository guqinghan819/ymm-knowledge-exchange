package wcg.yhm.ymm.count.enums;

import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-04
 * @Description:
 * @Version: 1.0
 */

@Getter
public enum CountTypeEnum {
    FOLLOW(1, "关注"),
    UNFOLLOW(0, "取关"),
    ;


    private final Integer value;
    private final String text;

    CountTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static CountTypeEnum getEnum(Integer value) {
        for (CountTypeEnum countTypeEnum : CountTypeEnum.values()) {
            if (countTypeEnum.value.equals(value)) {
                return countTypeEnum;
            }
        }
        return null;
    }
}
    
