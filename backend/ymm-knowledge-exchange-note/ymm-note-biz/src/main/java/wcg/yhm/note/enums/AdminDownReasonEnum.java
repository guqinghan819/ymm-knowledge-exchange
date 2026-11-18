package wcg.yhm.note.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum AdminDownReasonEnum {
    COMMUNITY_RULE_VIOLATION("违反社区规范"),
    MISLEADING_CONTENT("虚假或误导信息"),
    INFRINGEMENT("涉嫌侵权或未经授权转载"),
    SPAM_OR_PROMOTION("广告营销或恶意引流"),
    LOW_QUALITY("内容低质或与平台主题无关"),
    OTHER("其他");

    private final String label;

    public static AdminDownReasonEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.name().equals(code.trim()))
                .findFirst()
                .orElse(null);
    }
}
