package wcg.yhm.ymm.search.biz.enums;

import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 账号状态枚举
 * @Version: 1.0
 */

@Getter
public enum StatusEnum {
    ENABLE(0),
    DISABLED(1);

    private final Integer value;

     StatusEnum(Integer value) {
        this.value = value;
    }
}
    
