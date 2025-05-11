package com.jdmatchr.core.config;

import com.jdmatchr.core.service.UserDetailsServiceImpl; // Import your UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a Bean.
     * This AuthenticationManager is configured to use our custom UserDetailsService
     * and PasswordEncoder.
     *
     * @param http HttpSecurity to obtain the shared AuthenticationManagerBuilder.
     * @return The configured AuthenticationManager.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsServiceImpl) // Tell Spring Security to use your service to load users
                .passwordEncoder(passwordEncoder());      // Tell Spring Security which password encoder to use
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless APIs
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // Permit all requests to /api/v1/auth/** (for /register, /login)
                                // and /api/v1/users/ensure-oauth without authentication.
                                .requestMatchers("/api/v1/auth/**", "/api/v1/users/ensure-oauth").permitAll()
                                // Permit access to your test /hello endpoint (if you still have it)
                                .requestMatchers("/hello", "/").permitAll()
                                // For any other request, authentication is required.
                                // JWT validation will be added later for these.
                                .anyRequest().authenticated()
                )
                // Configure session management to be stateless, as JWTs will manage session state.
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Later, a JWT authentication filter will be added here to process Bearer tokens
        // for the .anyRequest().authenticated() routes.

        return http.build();
    }
}
