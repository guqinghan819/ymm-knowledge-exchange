package wcg.yhm.ai.biz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wcg.yhm.ai.api.ModerationRequest;
import wcg.yhm.ai.api.ModerationResult;
import wcg.yhm.ai.biz.service.NoteModerationService;

@RestController
@RequestMapping("/ai/internal/moderation")
@RequiredArgsConstructor
public class NoteModerationController {
    private final NoteModerationService service;

    @PostMapping("/review")
    public ModerationResult review(@RequestBody ModerationRequest request) {
        return service.review(request);
    }
}
