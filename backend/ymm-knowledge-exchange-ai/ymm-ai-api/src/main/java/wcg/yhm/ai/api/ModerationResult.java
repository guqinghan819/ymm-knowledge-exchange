package wcg.yhm.ai.api;

import lombok.Data;

@Data
public class ModerationResult {
    /** PASS, REJECT or REVIEW. */
    private String textVerdict;
    private String textReason;
    private boolean imagePass;
    /** Only shown in the administrator interface. */
    private String internalError;
}
