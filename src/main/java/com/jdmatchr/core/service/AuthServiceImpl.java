package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.EnsureOAuthRequest;
import com.jdmatchr.core.dto.RegisterRequest;
import com.jdmatchr.core.dto.UserResponse;
import com.jdmatchr.core.entity.Account;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.AccountRepository;
import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Not used for OAuth user creation directly
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // For returning the map
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder; // Still needed for credentials registration

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("AuthServiceImpl initialized (Database Persistence Enabled).");
    }

    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest registerRequest) {
        // ... (your existing registerUser logic remains here) ...
        if (registerRequest == null) {
            logger.warn("Registration request is null.");
            throw new IllegalArgumentException("Registration request cannot be null.");
        }
        logger.info("AuthService: Attempting to register user with email: {}", registerRequest.getEmail());
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Registration failed: Email address {} already in use.", registerRequest.getEmail());
            throw new RuntimeException("Error: Email address already in use: " + registerRequest.getEmail());
        }
        User newUser = new User();
        newUser.setName(registerRequest.getName());
        newUser.setEmail(registerRequest.getEmail());
        Account newAccount = new Account();
        newAccount.setProviderType("credentials");
        newAccount.setProviderId("credentials");
        newAccount.setProviderAccountId(registerRequest.getEmail());
        newAccount.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.addAccount(newAccount);
        User savedUser = userRepository.save(newUser);
        logger.info("User and associated account saved successfully for email: {}. User ID: {}", savedUser.getEmail(), savedUser.getId());
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    @Override
    @Transactional
    public Map<String, UUID> ensureOAuthUser(EnsureOAuthRequest ensureOAuthRequest) {
        if (ensureOAuthRequest == null) {
            throw new IllegalArgumentException("Ensure OAuth request cannot be null.");
        }
        logger.info("Attempting to ensure OAuth user: Provider={}, ProviderAccountId={}",
                ensureOAuthRequest.getProviderId(), ensureOAuthRequest.getProviderAccountId());

        // 1. Check if an account with this providerId and providerAccountId already exists
        Optional<Account> existingAccountOpt = accountRepository.findByProviderIdAndProviderAccountId(
                ensureOAuthRequest.getProviderId(),
                ensureOAuthRequest.getProviderAccountId()
        );

        User userToReturn;

        if (existingAccountOpt.isPresent()) {
            // Account exists, get the associated user
            logger.info("OAuth account found for ProviderAccountId: {}. Returning existing user.", ensureOAuthRequest.getProviderAccountId());
            userToReturn = existingAccountOpt.get().getUser();
            if (userToReturn == null) {
                // This would indicate a data integrity issue
                logger.error("CRITICAL: OAuth account exists but has no associated user. ProviderAccountId: {}", ensureOAuthRequest.getProviderAccountId());
                throw new RuntimeException("Data integrity issue: OAuth account found without a user.");
            }
            // Optional: Update user details (name, image) if they have changed from the provider
            boolean userUpdated = false;
            if (ensureOAuthRequest.getName() != null && !ensureOAuthRequest.getName().equals(userToReturn.getName())) {
                userToReturn.setName(ensureOAuthRequest.getName());
                userUpdated = true;
            }
            if (ensureOAuthRequest.getImageUrl() != null && !ensureOAuthRequest.getImageUrl().equals(userToReturn.getImageUrl())) {
                userToReturn.setImageUrl(ensureOAuthRequest.getImageUrl());
                userUpdated = true;
            }
            if (userUpdated) {
                userRepository.save(userToReturn);
                logger.info("Updated existing user details for OAuth user: {}", userToReturn.getEmail());
            }

        } else {
            // Account does not exist. We might need to create a new User or link to an existing User by email.
            // For simplicity in this MVP, let's assume if the OAuth account doesn't exist,
            // we check if a user with that email exists. If so, link. Otherwise, create new user.

            logger.info("OAuth account not found for ProviderAccountId: {}. Checking for user by email: {}",
                    ensureOAuthRequest.getProviderAccountId(), ensureOAuthRequest.getEmail());

            Optional<User> userByEmailOpt = userRepository.findByEmail(ensureOAuthRequest.getEmail());

            if (userByEmailOpt.isPresent()) {
                // User with this email already exists (e.g., signed up with credentials). Link this OAuth account to them.
                userToReturn = userByEmailOpt.get();
                logger.info("User found by email {}. Linking new OAuth account.", ensureOAuthRequest.getEmail());
            } else {
                // No user with this email. Create a new User.
                logger.info("No user found by email {}. Creating new user for OAuth.", ensureOAuthRequest.getEmail());
                User newUser = new User();
                newUser.setEmail(ensureOAuthRequest.getEmail());
                newUser.setName(ensureOAuthRequest.getName());
                newUser.setImageUrl(ensureOAuthRequest.getImageUrl());
                // emailVerified could be set to true or based on provider info
                // newUser.setEmailVerified(OffsetDateTime.now()); // Example
                userToReturn = userRepository.save(newUser); // Save the new user first
            }

            // Create the new Account for this OAuth provider
            Account newOAuthAccount = new Account();
            newOAuthAccount.setProviderId(ensureOAuthRequest.getProviderId());
            newOAuthAccount.setProviderType("oauth"); // Or use providerId if it's descriptive enough
            newOAuthAccount.setProviderAccountId(ensureOAuthRequest.getProviderAccountId());
            // No password hash for OAuth accounts
            userToReturn.addAccount(newOAuthAccount); // This links user to account and account to user
            userRepository.save(userToReturn); // Re-save user to persist the new account link if user was existing
            // Or if cascade is set up correctly, saving account might be enough if user already saved.
            // Saving user after addAccount ensures the relationship is persisted.
            logger.info("Created and linked new OAuth account for user: {}", userToReturn.getEmail());
        }

        if (userToReturn.getId() == null) {
            logger.error("CRITICAL: User ID is null after ensureOAuthUser process for email {}", ensureOAuthRequest.getEmail());
            throw new RuntimeException("Failed to obtain a valid user ID during OAuth synchronization.");
        }

        return Collections.singletonMap("userId", userToReturn.getId());
    }
}
