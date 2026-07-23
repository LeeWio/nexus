package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.service.INewsletterService;

import java.util.concurrent.TimeUnit;

@Tag(name = "Newsletter", description = "Public endpoints for blog newsletter subscriptions")
@RestController
@RequestMapping("/api/v1/public/newsletter")
@RequiredArgsConstructor
@Validated
public class NewsletterController {

	private final INewsletterService newsletterService;

	@PostMapping("/subscribe")
	@RateLimit(count = 3, time = 1, unit = TimeUnit.HOURS, message = "Too many subscription requests. Please try again later.")
	@Operation(summary = "Subscribe to newsletter", description = "Request to join the blog's newsletter mailing list.")
	public ApiResponse<Void> subscribe(
			@Email(message = "Email address is invalid") @NotBlank(message = "Email is required") @RequestParam String email) {
		return newsletterService.subscribe(email);
	}

	@GetMapping("/verify")
	@Operation(summary = "Verify subscription", description = "Verify and activate the newsletter subscription using a token.")
	public ApiResponse<Void> verify(
			@Parameter(description = "Verification token from email") @RequestParam String token) {
		return newsletterService.verify(token);
	}

	@GetMapping("/unsubscribe")
	@Operation(summary = "Unsubscribe", description = "Opt-out from the newsletter mailing list.")
	public ApiResponse<Void> unsubscribe(
			@Parameter(description = "Unsubscribe token from email footer") @RequestParam String token) {
		return newsletterService.unsubscribe(token);
	}
}
