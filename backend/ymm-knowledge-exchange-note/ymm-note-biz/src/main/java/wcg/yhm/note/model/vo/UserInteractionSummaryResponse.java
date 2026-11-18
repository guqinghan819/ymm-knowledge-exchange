package wcg.yhm.note.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInteractionSummaryResponse {

    private Long likedTotal;

    private Long collectedTotal;
}
