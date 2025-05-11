package com.jdmatchr.core.service; // Adjust package name if yours is different

import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
import com.jdmatchr.core.entity.Account;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.AccountRepository; // Make sure this is imported
import com.jdmatchr.core.repository.UserRepository;    // Make sure this is imported
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

// No need for java.util.UUID here if your entities handle ID generation

/**
 * Implementation of the {@link AuthService} interface.
 * Handles the business logic for user registration using database persistence.
 */
@Service // Marks this class as a Spring service component
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository; // Though not directly used in registerUser if cascading from User
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor-based dependency injection.
     * Spring will automatically inject instances of UserRepository, AccountRepository, and PasswordEncoder.
     */
    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("AuthServiceImpl initialized (Database Persistence Enabled).");
    }

    /**
     * Registers a new user by persisting their details to the database.
     * This method is transactional.
     *
     * @param registerRequest DTO containing registration details.
     * @return UserResponse DTO of the newly created user.
     * @throws RuntimeException (or a more specific custom exception like UserAlreadyExistsException)
     * if a user with the provided email already exists or if the request is invalid.
     */
    @Override
    @Transactional // Ensures that the entire method runs within a single database transaction.
    public UserResponse registerUser(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            logger.warn("Registration request is null.");
            throw new IllegalArgumentException("Registration request cannot be null.");
        }

        logger.info("AuthService: Attempting to register user with email: {}", registerRequest.getEmail());

        // 1. Check if user with the given email already exists using the repository
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Registration failed: Email address {} already in use.", registerRequest.getEmail());
            // In a real application, you'd throw a more specific custom exception.
            throw new RuntimeException("Error: Email address already in use: " + registerRequest.getEmail());
        }

        // 2. Create a new User entity
        User newUser = new User();
        newUser.setName(registerRequest.getName());
        newUser.setEmail(registerRequest.getEmail());
        // Timestamps (createdAt, updatedAt) will be set by @PrePersist in User entity

        // 3. Create a new Account entity for 'credentials'
        Account newAccount = new Account();
        newAccount.setProviderType("credentials"); // Standard type for email/password
        newAccount.setProviderId("credentials");   // Standard identifier for this type
        newAccount.setProviderAccountId(registerRequest.getEmail()); // Use email as the account ID for credentials
        newAccount.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword())); // Hash the password

        // 4. Link the Account to the User (bi-directional relationship)
        // The User.addAccount() method should handle setting both sides of the relationship.
        newUser.addAccount(newAccount);

        // 5. Save the User entity. Due to CascadeType.ALL on User.accounts (defined in User entity),
        // the associated Account entity will also be persisted automatically when the User is saved.
        User savedUser = userRepository.save(newUser);
        logger.info("User and associated account saved successfully for email: {}. User ID: {}", savedUser.getEmail(), savedUser.getId());

        // 6. Return a UserResponse DTO containing details of the persisted user
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }
}
