package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;

/**
 * Authentication Service Interface.
 */
public interface IAuthService {

    /**
     * Registers a new user.
     */
    ApiResponse<Void> register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT token.
     */
    ApiResponse<AuthResponse> login(LoginRequest request);

    /**
     * Returns the currently authenticated user entity.
     */
    ApiResponse<User> getCurrentUser();
}
