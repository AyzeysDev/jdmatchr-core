package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.LoginRequest;
import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.UserRepository;
import com.jdmatchr.core.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder; // Optional for stateless, but good practice
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager; // Injected for login
    private final UserRepository userRepository; // Injected to fetch user details

    @Autowired
    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          UserRepository userRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Attempting to register user with email: {}", registerRequest.getEmail());
            UserResponse userResponse = authService.registerUser(registerRequest);
            logger.info("User registered successfully: {}", userResponse.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (RuntimeException e) { // Catching specific exceptions from service is better
            logger.error("Registration failed for email {}: {}", registerRequest.getEmail(), e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Email address already in use")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            // Consider a more generic error for other runtime exceptions from the service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Registration failed due to invalid data or server error.");
        } catch (Exception e) {
            logger.error("An unexpected error occurred during registration for email {}: ", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
        }
    }

    /**
     * Handles POST requests to /api/v1/auth/login for user authentication.
     * This endpoint will be called by NextAuth's `authorize` function in the frontend.
     *
     * @param loginRequest The login request DTO containing email and password.
     * @return A ResponseEntity containing the UserResponse DTO on successful authentication (HTTP 200 OK),
     * or an error response (HTTP 401 Unauthorized) if authentication fails.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Attempting login for user: {}", loginRequest.getEmail());
        try {
            // 1. Attempt to authenticate the user using Spring Security's AuthenticationManager.
            // This will internally use your UserDetailsServiceImpl and PasswordEncoder.
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),    // Principal (username)
                            loginRequest.getPassword()  // Credentials (password)
                    )
            );

            // 2. If authentication is successful, the 'authentication' object will be populated.
            // For stateless APIs, setting it in SecurityContextHolder is optional but good practice
            // if any subsequent in-process logic relies on it (though less common for this specific flow).
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Fetch the full User details from the database to return in the response.
            // The principal in the 'authentication' object is what your UserDetailsService returned as username.
            String authenticatedEmail = authentication.getName();
            User user = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() -> {
                        // This should ideally not happen if authentication succeeded, implies data inconsistency.
                        logger.error("CRITICAL: User {} authenticated but not found in repository.", authenticatedEmail);
                        return new RuntimeException("User not found after successful authentication - data inconsistency");
                    });

            logger.info("User login successful: {}", authenticatedEmail);
            UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail());

            // NextAuth's authorize function expects the user object directly for success.
            return ResponseEntity.ok(userResponse);

        } catch (BadCredentialsException e) {
            // This exception is thrown by Spring Security if authentication fails (e.g., wrong password, user not found by UserDetailsService).
            logger.warn("Login failed for user {}: Invalid credentials.", loginRequest.getEmail());
            // Return a generic message to avoid revealing whether the username or password was specifically wrong.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        } catch (Exception e) {
            // Catch any other unexpected errors during the login process.
            logger.error("An unexpected error occurred during login for user {}: ", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login. Please try again later.");
        }
    }
}
