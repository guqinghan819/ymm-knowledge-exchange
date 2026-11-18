package wcg.yhm.ai.biz.agent.enums.pptx;

import lombok.Getter;

/**
 * ppt实例状态枚举 状态机
 */
@Getter
public enum PptInstanceStatus {

    /**
     * 初始化
     */
    INIT("INIT", "初始化"),
    /**
     * 需求澄清
     */
    REQUIREMENT("REQUIREMENT", "需求澄清"),
    /**
     * 信息收集
     */
    SEARCH("SEARCH", "信息收集"),
    /**
     * 大纲生成
     */
    OUTLINE("OUTLINE", "大纲生成"),
    /**
     * 模板选择
     */
    TEMPLATE("TEMPLATE", "模板选择"),
    /**
     * Schema生成
     */
    SCHEMA("SCHEMA", "Schema生成"),
    /**
     * PPT渲染
     */
    RENDER("RENDER", "PPT渲染"),
    /**
     * 完成
     */
    SUCCESS("SUCCESS", "完成"),
    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    PptInstanceStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static PptInstanceStatus fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (PptInstanceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
