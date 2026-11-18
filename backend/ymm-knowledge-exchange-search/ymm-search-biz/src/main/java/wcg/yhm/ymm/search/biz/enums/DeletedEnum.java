package wcg.yhm.ymm.search.biz.enums;

import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-23
 * @Description: 逻辑删除枚举
 * @Version: 1.0
 */

@Getter
public enum DeletedEnum {
    YES(true),
    NO(false);

    private final Boolean value;

    DeletedEnum(Boolean value) {
        this.value = value;
    }
}
    
