package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.OtpLoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;

/**
 * Authentication Service Interface.
 */
public interface IAuthService {

    /**
     * Registers a new user account.
     */
    ApiResponse<Void> registerAccount(RegisterRequest request);

    /**
     * Authenticates a user and returns security credentials.
     */
    ApiResponse<AuthResponse> authenticate(LoginRequest request);

    /**
     * Sends an OTP to the user's email for login.
     */
    ApiResponse<Void> sendOtp(String email);

    /**
     * Authenticates a user using an OTP.
     */
    ApiResponse<AuthResponse> loginWithOtp(OtpLoginRequest request);

    /**
     * Returns the currently authenticated user details.
     */
    ApiResponse<User> getAuthenticatedUser();
}
