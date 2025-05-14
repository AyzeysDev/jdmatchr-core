package com.jdmatchr.core.security; // Or your chosen package

import com.jdmatchr.core.service.UserDetailsServiceImpl;
import com.jdmatchr.core.util.JwtUtil;
import io.jsonwebtoken.Claims; // Import Claims
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import this
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsServiceImpl) {
        this.jwtUtil = jwtUtil;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String requestURI = request.getRequestURI();
        logger.info("JwtAuthenticationFilter processing request for: {}", requestURI);

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                logger.info("Extracted JWT from request header.");
                // Initial validation of the token's structure and signature/expiration
                if (jwtUtil.validateToken(jwt)) { // This calls validateToken(jwt, null)
                    logger.info("JWT successfully validated (structure, signature, expiration).");

                    // Extract email from the token to load UserDetails
                    // Your JWT payload contains an "email" claim.
                    String emailFromToken = jwtUtil.extractClaim(jwt, claims -> claims.get("email", String.class));
                    // String subjectUuidFromToken = jwtUtil.extractUsername(jwt); // This is the UUID (sub claim)

                    logger.info("Extracted email from token: '{}'", emailFromToken);
                    // logger.debug("Extracted subject (UUID) from token: '{}'", subjectUuidFromToken);


                    if (StringUtils.hasText(emailFromToken) && SecurityContextHolder.getContext().getAuthentication() == null) {
                        logger.info("Attempting to load UserDetails for email: {}", emailFromToken);
                        try {
                            UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(emailFromToken);
                            logger.info("UserDetails loaded successfully for email: {}", emailFromToken);

                            // Second validation: ensure token is valid for the loaded UserDetails
                            // (e.g., subject in token could be cross-checked if needed, but email match is primary here)
                            if (jwtUtil.validateToken(jwt, userDetails)) { // This call passes userDetails
                                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                                logger.info("Successfully authenticated user '{}' via JWT and set SecurityContext.", emailFromToken);
                            } else {
                                logger.info("JWT token was initially valid, but failed userDetails-specific validation for email '{}'. Token might be for a different user or context.", emailFromToken);
                            }
                        } catch (UsernameNotFoundException e) {
                            logger.warn("User not found for email '{}' extracted from JWT: {}", emailFromToken, e.getMessage());
                        } catch (Exception e) {
                            logger.error("Error loading UserDetails or validating token for email '{}': {}", emailFromToken, e.getMessage(), e);
                        }
                    } else if (!StringUtils.hasText(emailFromToken)){
                        logger.info("Could not extract 'email' claim from JWT, or it was empty.");
                    } else {
                        logger.debug("SecurityContextHolder already contains an authentication for this request.");
                    }
                } else {
                    logger.warn("Invalid JWT received (failed initial validation - structure, signature, or expiration).");
                }
            } else {
                logger.info("No JWT found in Authorization header for request: {}", requestURI);
            }
        } catch (Exception e) {
            // This catch block is for unexpected errors within the filter itself.
            // Specific JWT parsing errors are handled within jwtUtil.validateToken()
            logger.error("Unexpected error in JwtAuthenticationFilter: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
