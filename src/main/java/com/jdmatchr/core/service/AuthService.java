package com.jdmatchr.core.service; // Adjust package name if yours is different

import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
// Potentially import custom exceptions if you define them later
// import com.jdmatchr.core.exception.UserAlreadyExistsException;

/**
 * Interface for authentication-related services.
 * Defines the contract for operations like user registration and login.
 */
public interface AuthService {
    UserResponse registerUser(RegisterRequest registerRequest);

    // Login method will be implicitly handled by Spring Security's mechanisms
    // when we configure it fully for username/password.
    // The `authorize` function in NextAuth's CredentialsProvider will call
    // our backend's /api/v1/auth/login endpoint, which Spring Security will process.
    // So, an explicit login method here might not be directly called in the same way,
    // but the logic for validating credentials will reside within Spring Security's flow,
    // potentially using a UserDetailsService if we go that route.
    // For now, we focus on registration.
}
