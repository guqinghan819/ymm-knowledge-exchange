package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-15
 * @Description: 发布删除枚举
 * @Version: 1.0
 */

@AllArgsConstructor
@Getter
public enum NotePublishEnum {
    PUBLISH(1),
    DELETE(0),
    ;

    private final Integer code;

    public static NotePublishEnum getEnum(Integer code) {
        for (NotePublishEnum notePublishEnum : NotePublishEnum.values()) {
            if (notePublishEnum.getCode().equals(code)) {
                return notePublishEnum;
            }
        }
        return null;
    }
}
    
