package wcg.yhm.ymm.search.biz.enums;

import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 笔记可见状态枚举
 * @Version: 1.0
 */

@Getter
public enum NoteVisibleEnum {
    PUBLIC(0, "所有人可见"),
    PRIVATE(1, "仅自己可见");


    private final Integer value;
    private final String text;

    NoteVisibleEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }
}
    
