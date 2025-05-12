package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.ApiErrorResponse; // Import the error DTO
import com.jdmatchr.core.dto.LoginRequest;
import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.UserRepository;
import com.jdmatchr.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest; // For getting request path
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          UserRepository userRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        try {
            logger.info("Attempting to register user with email: {}", registerRequest.getEmail());
            UserResponse userResponse = authService.registerUser(registerRequest);
            logger.info("User registered successfully with email: {}", userResponse.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (RuntimeException e) {
            logger.error("Registration failed for email {}: {}", registerRequest.getEmail(), e.getMessage());
            ApiErrorResponse errorResponse;
            if (e.getMessage() != null && e.getMessage().contains("Email address already in use")) {
                errorResponse = new ApiErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", e.getMessage(), request.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
            // For other validation-like errors thrown by the service
            errorResponse = new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", e.getMessage(), request.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during registration for email {}: ", registerRequest.getEmail(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred. Please try again later.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        logger.info("Attempting login for user: {}", loginRequest.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String authenticatedEmail = authentication.getName();
            User user = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() -> {
                        logger.error("CRITICAL: User {} authenticated but not found in repository.", authenticatedEmail);
                        return new RuntimeException("User data inconsistency after successful authentication.");
                    });

            logger.info("User login successful: {}", authenticatedEmail);
            UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail());
            return ResponseEntity.ok(userResponse);

        } catch (BadCredentialsException e) {
            logger.warn("Login failed for user {}: Invalid credentials.", loginRequest.getEmail());
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Invalid email or password.", // User-friendly message
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (RuntimeException e) { // Catch specific runtime errors like the one from orElseThrow
            logger.error("Runtime error during login for user {}: ", loginRequest.getEmail(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    e.getMessage(), // Or a more generic message
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        catch (Exception e) {
            logger.error("An unexpected error occurred during login for user {}: ", loginRequest.getEmail(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An error occurred during login. Please try again later.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
