package com.jdmatchr.core.config;

import com.jdmatchr.core.security.JwtAuthenticationFilter; // Adjust package if you chose differently
import com.jdmatchr.core.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsServiceImpl; // UserDetailsService is used by AuthenticationManager
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a Bean.
     * This is automatically configured by Spring Boot 3+ using AuthenticationConfiguration
     * if you have a UserDetailsService and PasswordEncoder bean.
     * The AuthenticationManagerBuilder approach is still valid but this is more modern.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection as we are using JWTs and the API is stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules for HTTP requests
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // Permit all requests to authentication and OAuth user synchronization endpoints
                                .requestMatchers(
                                        "/api/v1/auth/**",
                                        "/api/v1/users/ensure-oauth"
                                ).permitAll()
                                // Permit access to any test/public root endpoints
                                .requestMatchers("/hello", "/").permitAll()
                                // All other requests must be authenticated
                                .anyRequest().authenticated()
                )

                // Configure session management to be stateless
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add the custom JWT authentication filter before the standard UsernamePasswordAuthenticationFilter
                // This ensures that JWTs are processed for authentication for requests that require it.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
