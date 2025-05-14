package com.jdmatchr.core.service;

import com.jdmatchr.core.entity.Account;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.AccountRepository;
import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    // A structurally valid, but insecure, dummy BCrypt hash.
    // DO NOT USE THIS FOR ACTUAL PASSWORD STORAGE. It's a placeholder.
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZemOkllW";

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.info("Attempting to load user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.info("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        Account accountToUse = accountRepository.findByUserAndProviderId(user, "credentials")
                .orElseGet(() -> {
                    // This block is for users authenticated via JWT who might not have a "credentials" account
                    // (e.g., they signed up via OAuth). We still need to provide UserDetails.
                    // The password hash here is a non-null placeholder because Spring Security's User object requires one,
                    // but it won't be used for validation in a JWT flow (token signature is key).
                    if (user.getAccounts() != null && !user.getAccounts().isEmpty()) {
                        logger.info("User {} found, but no 'credentials' account. User likely authenticated via OAuth/JWT. Using dummy password hash for UserDetails.", email);
                        Account tempAccount = new Account();
                        tempAccount.setUser(user); // Associate with the user
                        tempAccount.setProviderId("jwt"); // Indicate this is for a JWT context
                        tempAccount.setProviderType("jwt");
                        tempAccount.setProviderAccountId(email);
                        tempAccount.setPasswordHash(DUMMY_PASSWORD_HASH);
                        return tempAccount;
                    }
                    // If user has NO accounts at all, this is problematic.
                    logger.error("No suitable account (credentials or otherwise) found for user: {}. This might indicate an issue with user setup.", email);
                    throw new UsernameNotFoundException("No suitable account for authentication for user: " + email);
                });

        logger.info("User found with email: {}. Preparing UserDetails.", email);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                accountToUse.getPasswordHash(), // Use the hash from the determined account
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
