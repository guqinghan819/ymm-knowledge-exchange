package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-09
 * @Description: lua脚本返回结果枚举
 * @Version: 1.0
 */

@Getter
@AllArgsConstructor
public enum LuaResultEnum {

    BLOOM_NOT_EXISTS(-1L, "布隆过滤器不存在"),
    NOTE_ALREADY_LIKE(1L, "笔记已经点赞"),
    SUCCESS(0L, "成功"),
    ZSET_NOT_EXISTS(2L, "zSet不存在"),
    NOTE_NOT_LIKE(3L, "笔记未点赞"),
    ;

    private final Long code;
    private final String text;

    public static LuaResultEnum getEnumByCode(Long code) {
        for (LuaResultEnum result : LuaResultEnum.values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }
}
    
