package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-10
 * @Description:
 * @Version: 1.0
 */

@Getter
@AllArgsConstructor
public enum BloomUnlikeEnum {
    BLOOM_NOT_EXISTS(-1L, "布隆过滤器不存在"),
    BLOOM_ALREADY_LIKE(1L, "笔记已经点赞"),
    BLOOM_NOT_LIKE(0L, "笔记没有点赞"),
    ;


    private final Long code;
    private final String text;

    public static BloomUnlikeEnum getEnumByCode(Long code) {
        for (BloomUnlikeEnum result : BloomUnlikeEnum.values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }
}
    
