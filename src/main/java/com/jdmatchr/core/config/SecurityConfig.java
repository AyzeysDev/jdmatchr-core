package com.jdmatchr.core.config; // Adjust package name if yours is different

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // For disabling CSRF in newer Spring Security
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Indicates that this class contains Spring configuration beans
@EnableWebSecurity // Enables Spring Security's web security support
public class SecurityConfig {

    /**
     * Defines a PasswordEncoder bean that uses BCrypt hashing algorithm.
     * BCrypt is a strong hashing algorithm that includes a salt to protect against rainbow table attacks.
     * This bean will be available for dependency injection wherever password encoding is needed (e.g., in your AuthService).
     *
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the security filter chain that applies to HTTP requests.
     * This is where you define which endpoints are public, which require authentication,
     * how authentication is performed, CSRF protection, session management, etc.
     *
     * @param http The HttpSecurity object to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery) protection.
                // CSRF protection is typically needed for browser-based sessions with cookies.
                // For stateless REST APIs that use token-based authentication (like JWTs from NextAuth),
                // and where the frontend and backend might be on different domains, CSRF is often disabled.
                // Ensure you understand the implications if your API is called directly from browsers in a stateful way.
                .csrf(AbstractHttpConfigurer::disable) // Modern way to disable CSRF

                // Configure authorization rules for HTTP requests.
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // Permit all requests to /api/v1/auth/** (for /register, /login)
                                // and /api/v1/users/ensure-oauth without authentication.
                                .requestMatchers("/api/v1/auth/**", "/api/v1/users/ensure-oauth").permitAll()
                                // Permit access to your test /hello endpoint (if you still have it)
                                .requestMatchers("/hello", "/").permitAll()
                                // For any other request, authentication is required.
                                // We will configure JWT validation for these later.
                                .anyRequest().authenticated()
                )

                // Configure session management.
                // For a stateless API backend that relies on tokens (like JWTs from NextAuth),
                // we typically set session creation to STATELESS.
                // This means Spring Security will not create or use HTTP sessions.
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Later, you will add configuration here for JWT validation if Spring Boot
        // needs to validate tokens for its own protected resources.
        // For now, NextAuth handles the primary token validation for calls proxied through it.
        // If Spring Boot endpoints are called directly by the frontend with a JWT,
        // then Spring Boot needs to validate that JWT.

        return http.build();
    }

    // Optional: If you need to configure WebSecurity for ignoring static resources,
    // but for a pure API backend, this is often not necessary.
    // @Bean
    // public WebSecurityCustomizer webSecurityCustomizer() {
    //     return (web) -> web.ignoring().requestMatchers("/css/**", "/js/**", "/images/**");
    // }
}
