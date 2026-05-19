package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.OtpLoginRequest;
import space.nebula.nexus.payload.request.OtpSendRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.service.IAuthService;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    @Resource
    private IAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account in PENDING status. Requires email verification/audit.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already exists", content = @Content(schema = @Schema(implementation = space.nebula.nexus.common.ApiResponse.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @RateLimit(count = 3, time = 1, unit = TimeUnit.HOURS, message = "Registration frequency too high. Please try again in an hour.")
    public space.nebula.nexus.common.ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerAccount(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Standard login", description = "Authenticate using username/email and password to receive a JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account locked or disabled")
    })
    @RateLimit(count = 10, time = 1, unit = TimeUnit.MINUTES, message = "Login attempts too frequent. Please wait a moment.")
    public space.nebula.nexus.common.ApiResponse<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request);
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Sends a 6-digit verification code to the registered email address.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent"),
        @ApiResponse(responseCode = "404", description = "Email not found")
    })
    @RateLimit(count = 1, time = 1, unit = TimeUnit.MINUTES, message = "Please wait a minute before requesting another OTP.")
    public space.nebula.nexus.common.ApiResponse<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return authService.sendOtp(request.email());
    }

    @PostMapping("/otp/login")
    @Operation(summary = "OTP login", description = "Login using the code sent to your email. No password required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired OTP")
    })
    @RateLimit(count = 5, time = 5, unit = TimeUnit.MINUTES, message = "Too many OTP login attempts. Please try again later.")
    public space.nebula.nexus.common.ApiResponse<AuthResponse> loginWithOtp(@Valid @RequestBody OtpLoginRequest request) {
        return authService.loginWithOtp(request);
    }
}
