package wcg.yhm.note.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BloomNoteCollectEnum {

    BLOOM_NOTE_EXISTS(-1L, "bloom不存在"),
    BLOOM_NOTE_ALREADY_COLLECT(1L, "bloom中已经收藏该笔记"),
    BLOOM_SUCCESS(0L, "bloom操作成功"),
    BLOOM_NOTE_NOT_COLLECT(2L, "bloom中没有该笔记id")
    ;
    private final Long code;
    private final String text;

    public static BloomNoteCollectEnum getByCode(Long code) {
        for (BloomNoteCollectEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
