package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.INewsletterService;

@Tag(name = "Newsletter", description = "Public endpoints for blog newsletter subscriptions")
@RestController
@RequestMapping("/api/v1/public/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final INewsletterService newsletterService;

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to newsletter", description = "Request to join the blog's newsletter mailing list.")
    public ApiResponse<Void> subscribe(@RequestParam String email) {
        return newsletterService.subscribe(email);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify subscription", description = "Verify and activate the newsletter subscription using a token.")
    public ApiResponse<Void> verify(@Parameter(description = "Verification token from email") @RequestParam String token) {
        return newsletterService.verify(token);
    }

    @GetMapping("/unsubscribe")
    @Operation(summary = "Unsubscribe", description = "Opt-out from the newsletter mailing list.")
    public ApiResponse<Void> unsubscribe(@Parameter(description = "Unsubscribe token from email footer") @RequestParam String token) {
        return newsletterService.unsubscribe(token);
    }
}
