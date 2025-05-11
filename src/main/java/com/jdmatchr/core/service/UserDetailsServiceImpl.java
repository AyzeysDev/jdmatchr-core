package com.jdmatchr.core.service;

import com.jdmatchr.core.entity.Account;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.AccountRepository;
import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // For authorities if you add roles later
import java.util.Optional;

@Service("userDetailsService") // Explicitly name the bean if needed, or just @Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true) // Use readOnly for read operations
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by email: {}", email);

        // 1. Find the User entity by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // 2. Find the 'credentials' Account associated with this User
        // Assuming a user has one 'credentials' account for password-based login.
        Account credentialsAccount = accountRepository.findByUserAndProviderId(user, "credentials")
                .orElseThrow(() -> {
                    // This case should ideally not happen if a user registered with email/password
                    // but their credentials account is missing.
                    logger.error("Credentials account not found for user: {}", email);
                    return new UsernameNotFoundException("Credentials account not found for user: " + email);
                });

        if (credentialsAccount.getPasswordHash() == null || credentialsAccount.getPasswordHash().isEmpty()) {
            logger.error("Password hash is missing for credentials account of user: {}", email);
            throw new UsernameNotFoundException("User account is not configured for password authentication: " + email);
        }

        logger.info("User found with email: {}. Preparing UserDetails.", email);

        // 3. Create and return a Spring Security UserDetails object
        // The username in UserDetails is typically the email.
        // The password is the HASHED password from the database.
        // Authorities would be user roles (e.g., "ROLE_USER", "ROLE_ADMIN"). For now, an empty list.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Username used by Spring Security
                credentialsAccount.getPasswordHash(), // HASHED password
                new ArrayList<>() // Authorities (roles) - empty for now
                // You can add more flags like:
                // true, // enabled
                // true, // accountNonExpired
                // true, // credentialsNonExpired
                // true, // accountNonLocked
        );
    }
}
