package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    @Resource
    private IAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @RateLimit(count = 3, time = 1, unit = TimeUnit.HOURS, message = "Registration frequency too high. Please try again in an hour.")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerAccount(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT token")
    @RateLimit(count = 10, time = 1, unit = TimeUnit.MINUTES, message = "Login attempts too frequent. Please wait a moment.")
    public ApiResponse<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request);
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send an OTP code to user's email")
    @RateLimit(count = 1, time = 1, unit = TimeUnit.MINUTES, message = "Please wait a minute before requesting another OTP.")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        return authService.sendOtp(request.email());
    }

    @PostMapping("/otp/login")
    @Operation(summary = "Login using email and OTP code")
    @RateLimit(count = 5, time = 5, unit = TimeUnit.MINUTES, message = "Too many OTP login attempts. Please try again later.")
    public ApiResponse<AuthResponse> loginWithOtp(@Valid @RequestBody OtpLoginRequest request) {
        return authService.loginWithOtp(request);
    }
}
