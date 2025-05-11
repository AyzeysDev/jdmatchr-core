package com.jdmatchr.core.service; // Adjust package name if yours is different

import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
// Comment out entity and repository imports if not used in this temporary version
// import com.jdmatchr.core.entity.Account;
// import com.jdmatchr.core.entity.User;
// import com.jdmatchr.core.repository.AccountRepository;
// import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
// Transactional annotation is not strictly needed if no DB operations occur
// import org.springframework.transaction.annotation.Transactional;

import java.util.UUID; // For generating a mock UUID

/**
 * Simplified Implementation of the {@link AuthService} interface for basic testing.
 * This version does NOT interact with a database and returns mock responses.
 * It's intended for use when database auto-configuration is excluded.
 */
@Service // Marks this class as a Spring service component
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    // Repositories are injected but will not be used in this simplified version
    // private final UserRepository userRepository;
    // private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor-based dependency injection.
     * Spring will automatically inject PasswordEncoder.
     * UserRepository and AccountRepository would be injected if not commented out.
     */
    @Autowired
    public AuthServiceImpl(
            // UserRepository userRepository, // Temporarily comment out if not used
            // AccountRepository accountRepository, // Temporarily comment out if not used
            PasswordEncoder passwordEncoder) {
        // this.userRepository = userRepository;
        // this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("AuthServiceImpl initialized (Simplified for Basic Testing - NO DB INTERACTIONS).");
    }

    /**
     * MOCK Registers a new user.
     * This method simulates registration without database interaction.
     *
     * @param registerRequest DTO containing registration details.
     * @return UserResponse DTO with mock data.
     */
    @Override
    // @Transactional // Not needed as there are no actual DB transactions
    public UserResponse registerUser(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            logger.warn("Registration request is null.");
            throw new IllegalArgumentException("Registration request cannot be null.");
        }

        logger.info("AuthService: registerUser called for email: {} (MOCKED - NO DB INTERACTION)", registerRequest.getEmail());

        // 1. Simulate checking for existing user (always assume user doesn't exist for this mock)
        // if (userRepository.existsByEmail(registerRequest.getEmail())) {
        //     throw new RuntimeException("Error: Email address already in use: " + registerRequest.getEmail());
        // }
        // For mock, let's simulate a conflict for a specific email to test controller error handling
        if ("conflict@example.com".equals(registerRequest.getEmail())) {
            logger.warn("Simulating email conflict for: {}", registerRequest.getEmail());
            throw new RuntimeException("Email address already in use: " + registerRequest.getEmail());
        }


        // 2. "Hash" the password (the operation is performed, but not stored)
        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        logger.debug("Password for {} would be hashed to: {} (not stored)", registerRequest.getEmail(), hashedPassword);

        // 3. Create and return a mock UserResponse DTO
        // Generate a random UUID for the mock user ID
        UUID mockUserId = UUID.randomUUID();
        UserResponse mockResponse = new UserResponse(
                mockUserId,
                registerRequest.getName(),
                registerRequest.getEmail()
        );

        logger.info("Mock registration successful for email: {}. Mock User ID: {}", mockResponse.getEmail(), mockResponse.getId());
        return mockResponse;
    }
}
