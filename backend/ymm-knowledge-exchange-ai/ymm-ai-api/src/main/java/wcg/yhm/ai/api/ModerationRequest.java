package wcg.yhm.ai.api;

import lombok.Data;
import java.util.List;

@Data
public class ModerationRequest {
    private String title;
    private String content;
    private List<String> imageUrls;
    private boolean textUnavailable;
}
