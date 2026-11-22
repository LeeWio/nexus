package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.service.MomentSubscriptionService;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/moments/subscription")
@RequiredArgsConstructor
@Validated
@Tag(name = "Moment Subscriptions", description = "Email subscriptions to new public Moments")
public class MomentSubscriptionController {
	private final MomentSubscriptionService subscriptions;

	@PostMapping("/subscribe")
	@RateLimit(count = 3, time = 1, unit = TimeUnit.HOURS, message = "Too many subscription requests. Please try again later.")
	@Operation(summary = "Request a Moments subscription confirmation email")
	public ApiResponse<Void> subscribeToMoments(@Valid @RequestBody EmailRequest request, Locale locale) {
		subscriptions.subscribe(request.email(), locale);
		return ApiResponse.success("Confirmation instructions will be sent if the address is eligible.", null);
	}

	@PostMapping("/verify")
	@Operation(summary = "Confirm a Moments email subscription")
	public ApiResponse<Void> verifyMomentSubscription(@Valid @RequestBody TokenRequest request) {
		subscriptions.verify(request.token());
		return ApiResponse.success();
	}

	@PostMapping("/unsubscribe")
	@Operation(summary = "Unsubscribe from Moments emails")
	public ApiResponse<Void> unsubscribeFromMoments(@Valid @RequestBody TokenRequest request) {
		subscriptions.unsubscribe(request.token());
		return ApiResponse.success();
	}

	@PostMapping(value = "/one-click", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
			MediaType.MULTIPART_FORM_DATA_VALUE})
	@Operation(summary = "Process an RFC 8058 one-click Moments unsubscribe request")
	public ApiResponse<Void> unsubscribeFromMomentsOneClick(@RequestParam @Size(min = 36, max = 36) String token,
			@RequestParam("List-Unsubscribe") String action) {
		if (!"One-Click".equals(action)) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "One-click unsubscribe requires confirmation");
		}
		subscriptions.unsubscribe(token);
		return ApiResponse.success();
	}

	public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {
	}

	public record TokenRequest(@NotBlank @Size(min = 36, max = 36) String token) {
	}
}
