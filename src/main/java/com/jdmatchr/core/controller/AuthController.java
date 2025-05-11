package com.jdmatchr.core.controller; // Adjust package name if yours is different

import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
import com.jdmatchr.core.service.AuthService;
import jakarta.validation.Valid; // For validating the request body
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling authentication-related requests,
 * such as user registration and login.
 */
@RestController // Marks this class as a REST controller, combining @Controller and @ResponseBody
@RequestMapping("/api/v1/auth") // Base path for all endpoints in this controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructor-based dependency injection for the AuthService.
     *
     * @param authService The authentication service.
     */
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles POST requests to /api/v1/auth/register for new user registration.
     *
     * @param registerRequest The registration request DTO, validated against its constraints.
     * @return A ResponseEntity containing the UserResponse DTO on successful registration (HTTP 201 Created),
     * or an error response if registration fails (e.g., HTTP 400 Bad Request, HTTP 409 Conflict).
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // The @Valid annotation triggers validation of the RegisterRequest DTO
        // based on the constraints defined within it (e.g., @NotBlank, @Email, @Size).
        // If validation fails, Spring Boot will automatically return a 400 Bad Request
        // response, often handled by a global exception handler (which we can add later).

        try {
            logger.info("Attempting to register user with email: {}", registerRequest.getEmail());
            UserResponse userResponse = authService.registerUser(registerRequest);
            logger.info("User registered successfully: {}", userResponse.getEmail());
            // Return HTTP 201 Created status with the created user's details in the body
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (RuntimeException e) { // Catching RuntimeException from AuthService (e.g., email already exists)
            logger.error("Registration failed: {}", e.getMessage());
            // For an email already in use, a 409 Conflict is appropriate.
            // For other runtime exceptions from the service, a 400 Bad Request or 500 Internal Server Error might be suitable.
            // This simple catch can be refined with more specific exception handling.
            if (e.getMessage() != null && e.getMessage().contains("Email address already in use")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            }
            // For other validation-like errors thrown by the service (though @Valid should catch DTO issues)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Catch any other unexpected errors
            logger.error("An unexpected error occurred during registration: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again later.");
        }
    }

    // The /api/v1/auth/login endpoint will be primarily handled by Spring Security
    // once we configure it to use a custom UserDetailsService (which would use UserRepository).
    // NextAuth's `authorize` function will call this endpoint, and Spring Security's
    // authentication filters will process the credentials.
    // You don't typically write an explicit @PostMapping("/login") method here that
    // manually calls a service to "log in" if you're using Spring Security's formLogin
    // or httpBasic, or a custom authentication filter chain for username/password.
    // However, if you needed to return a custom response *after* Spring Security authenticates,
    // you might have a controller method, but the authentication itself is by the framework.
    // Since NextAuth expects user details back, Spring Security's success handler
    // will need to provide that (often by default if UserDetailsService returns a UserDetails object).
}
