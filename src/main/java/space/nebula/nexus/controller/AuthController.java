package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.OtpLoginRequest;
import space.nebula.nexus.payload.request.OtpSendRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.service.IAuthService;

import java.util.concurrent.TimeUnit;

/**
 * Controller for user authentication and account management. Provides
 * registration, standard login, and OTP-based login mechanisms.
 */
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and security")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final IAuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Creates a new user account in PENDING status. Requires administrator approval.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or account already exists", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Registration rate limit exceeded")})
	@RateLimit(count = 3, time = 1, unit = TimeUnit.HOURS, message = "Registration frequency too high. Please try again in an hour.")
	public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
		return authService.registerAccount(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Standard login", description = "Authenticate using credentials to receive a JWT access token.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid username or password"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Account locked or disabled")})
	@RateLimit(count = 10, time = 1, unit = TimeUnit.MINUTES, message = "Login attempts too frequent. Please wait a moment.")
	public ApiResponse<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
		return authService.authenticate(request);
	}

	@PostMapping("/otp/send")
	@Operation(summary = "Send OTP", description = "Sends a verification code to the registered email address.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP code sent successfully"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email address not found")})
	@RateLimit(count = 1, time = 1, unit = TimeUnit.MINUTES, message = "Please wait a minute before requesting another OTP.")
	public ApiResponse<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
		return authService.sendOtp(request.email());
	}

	@PostMapping("/otp/login")
	@Operation(summary = "OTP login", description = "Login using a one-time password code sent via email.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP Login successful"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired OTP code")})
	@RateLimit(count = 5, time = 5, unit = TimeUnit.MINUTES, message = "Too many OTP login attempts. Please try again later.")
	public ApiResponse<AuthResponse> loginWithOtp(@Valid @RequestBody OtpLoginRequest request) {
		return authService.loginWithOtp(request);
	}
}
