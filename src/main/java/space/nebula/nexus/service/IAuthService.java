package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.OtpLoginRequest;
import space.nebula.nexus.payload.request.PasswordResetConfirmRequest;
import space.nebula.nexus.payload.request.PasswordResetRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.security.model.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;

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
	 * Requests a password-reset OTP without revealing whether the email belongs to
	 * an account.
	 */
	ApiResponse<Void> requestPasswordReset(PasswordResetRequest request);

	/**
	 * Resets a password after atomically consuming a valid password-reset OTP.
	 */
	ApiResponse<Void> confirmPasswordReset(PasswordResetConfirmRequest request);

	/**
	 * Returns the currently authenticated user details.
	 */
	ApiResponse<User> getAuthenticatedUser();

	/**
	 * Logs out the current user, invalidating the session/token.
	 */
	ApiResponse<Void> logout(HttpServletRequest request);

	/**
	 * Refreshes an access token using a refresh token.
	 */
	ApiResponse<AuthResponse> refreshToken(String refreshToken);

	/**
	 * Issues a fresh access/refresh token pair for an already-authenticated user
	 * (used by OAuth success handling).
	 */
	AuthResponse issueTokens(SecurityUser securityUser);

	/**
	 * Exchanges a one-time OAuth login code for an auth response.
	 */
	ApiResponse<AuthResponse> exchangeOAuthLoginCode(String code);
}
