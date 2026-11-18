package wcg.yhm.note.enums;

import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description: 笔记类型枚举(图文 ， 视频)
 * @Version: 1.0
 */

@Getter
public enum NoteTypeEnum {
    IMAGE(0, "图文"),
    VIDEO(1, "视频");


    private final Integer value;
    private final String text;

    NoteTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 判断类型是否有效
     * @param typeValue 上传类型值
     * @return
     */
    public static boolean isValid(Integer typeValue) {
        NoteTypeEnum[] typeEnums = NoteTypeEnum.values();
        for (NoteTypeEnum typeEnum : typeEnums) {
            if (typeValue.equals(typeEnum.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据类型值获取对应枚举类
     * @param typeValue
     * @return
     */
    public static NoteTypeEnum getNoteTypeEnum(Integer typeValue) {
        NoteTypeEnum[] typeEnums = NoteTypeEnum.values();
        for (NoteTypeEnum typeEnum : typeEnums) {
            if (typeValue.equals(typeEnum.value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
    
