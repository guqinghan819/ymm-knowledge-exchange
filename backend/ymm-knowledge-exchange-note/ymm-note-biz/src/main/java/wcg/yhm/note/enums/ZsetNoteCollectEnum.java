package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-12
 * @Description:
 * @Version: 1.0
 */
@AllArgsConstructor
@Getter
public enum ZsetNoteCollectEnum {
    ZSET_NOT_EXISTS(-1L, "zset不存在"),
    SUCCESS(0L, "成功"),
    ELEMENT_ALREADY_EXISTS(1L, "元素已经存在");

    private final Long code;
    private final String message;

    // 通过code获取对应的枚举实例
    public static ZsetNoteCollectEnum getByCode(Long code) {
        for (ZsetNoteCollectEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
    
