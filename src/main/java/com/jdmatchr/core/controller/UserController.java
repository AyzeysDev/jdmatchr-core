package com.jdmatchr.core.controller; // Or com.jdmatchr.core.controller if adding to AuthController

import com.jdmatchr.core.dto.ApiErrorResponse;
import com.jdmatchr.core.dto.EnsureOAuthRequest;
import com.jdmatchr.core.service.AuthService; // Assuming ensureOAuthUser is in AuthService
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users") // Changed base path for user-specific operations
public class UserController { // Renamed to UserController, or merge into AuthController

    private static final Logger logger = LoggerFactory.getLogger(UserController.class); // Or AuthController.class

    private final AuthService authService; // Assuming ensureOAuthUser is part of AuthService

    @Autowired
    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint to ensure an OAuth user exists in the system.
     * Called by NextAuth backend after successful OAuth provider authentication.
     *
     * @param ensureOAuthRequest DTO containing OAuth provider details.
     * @param request            The HttpServletRequest.
     * @return A ResponseEntity containing a map with the internal "userId", or an error.
     */
    @PostMapping("/ensure-oauth")
    public ResponseEntity<?> ensureOAuthUser(@Valid @RequestBody EnsureOAuthRequest ensureOAuthRequest, HttpServletRequest request) {
        try {
            logger.info("Received /ensure-oauth request for provider: {}, providerAccountId: {}",
                    ensureOAuthRequest.getProviderId(), ensureOAuthRequest.getProviderAccountId());

            Map<String, UUID> result = authService.ensureOAuthUser(ensureOAuthRequest);

            if (result != null && result.containsKey("userId") && result.get("userId") != null) {
                logger.info("Successfully ensured OAuth user. Internal userId: {}", result.get("userId"));
                return ResponseEntity.ok(result);
            } else {
                logger.error("/ensure-oauth: AuthService returned null or invalid result for providerAccountId: {}", ensureOAuthRequest.getProviderAccountId());
                ApiErrorResponse errorResponse = new ApiErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "User Sync Error",
                        "Failed to retrieve a valid user ID after OAuth synchronization.",
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("/ensure-oauth bad request: {}", e.getMessage());
            ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", e.getMessage(), request.getRequestURI());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (RuntimeException e) { // Catch more specific exceptions if thrown by service
            logger.error("/ensure-oauth runtime error: {}", e.getMessage(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Processing Error", e.getMessage(), request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("/ensure-oauth unexpected error: {}", e.getMessage(), e);
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred during OAuth user synchronization.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
