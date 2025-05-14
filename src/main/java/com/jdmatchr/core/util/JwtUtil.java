package com.jdmatchr.core.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
// import io.jsonwebtoken.SignatureAlgorithm; // Not directly used for parsing/validation here
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            logger.warn("JWT secret is not configured or is too short. Using a default insecure key. THIS IS NOT SAFE FOR PRODUCTION.");
            this.secret = "DefaultInsecureSecretKeyForDevelopmentEnvironmentOnly1234567890";
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        logger.info("JWT Signing Key initialized.");
    }

    // Extracts the subject (typically user ID like UUID) from JWT token
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts the email claim from JWT token
    public String extractEmail(String token) {
        try {
            return extractClaim(token, claims -> claims.get("email", String.class));
        } catch (Exception e) {
            logger.warn("Could not extract 'email' claim from token or it was not a String.", e);
            return null;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            logger.warn("Token is expired: {}", e.getMessage());
            return true; // Token is explicitly expired
        } catch (Exception e) {
            logger.error("Error extracting expiration or checking if token expired: {}", e.getMessage());
            return true; // Treat as expired if an error occurs here
        }
    }

    /**
     * Validates the JWT token.
     * If userDetails is provided, it also checks if the email in the token matches userDetails.getUsername().
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        logger.info("Validating token. UserDetails provided: {}", (userDetails != null));
        if (userDetails != null) {
            logger.info("UserDetails username (expected email): {}", userDetails.getUsername());
        }

        try {
            // First, check basic token validity (signature, expiration, format)
            // extractAllClaims will throw an exception if the token is invalid per se
            extractAllClaims(token); // This implicitly checks signature and format
            System.out.println(token);
            if (isTokenExpired(token)) {
                logger.warn("Token validation failed: Token is expired.");
                return false;
            }

            // If userDetails are provided, perform an additional check
            if (userDetails != null) {
                final String emailFromToken = extractEmail(token);
                System.out.println(userDetails);
                System.out.println(emailFromToken);
                logger.info("Email extracted from token for comparison: {}", emailFromToken);
                if (emailFromToken != null && emailFromToken.equals(userDetails.getUsername())) {
                    logger.info("Token email matches UserDetails username. Token is valid for this user.");
                    return true;
                } else {
                    logger.warn("Token validation failed: Email in token ('{}') does not match UserDetails username ('{}').",
                            emailFromToken, userDetails.getUsername());
                    return false;
                }
            }

            // If userDetails is null, and we've reached here, the token is structurally valid and not expired.
            logger.info("Token is structurally valid and not expired (UserDetails not provided for specific user check).");
            return true;

        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token (malformed): {}", ex.getMessage());
        } catch (ExpiredJwtException ex) { // Should be caught by isTokenExpired, but good to have
            logger.error("Expired JWT token (caught directly in validateToken): {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty or argument is invalid: {}", ex.getMessage());
        } catch (Exception ex) { // Catch-all for any other parsing/validation errors
            logger.error("Unexpected error validating token: {}", ex.getMessage());
        }
        return false;
    }

    // Overload for validating token without UserDetails (checks structure, signature, expiration only)
    public Boolean validateToken(String token) {
        return validateToken(token, null);
    }

    // Renamed original extractUsername to extractSubject for clarity
    // as it extracts the 'sub' claim, which is the UUID in your case.
    // The filter will now use extractEmail.
    @Deprecated
    // This method is deprecated because the filter now uses email for UserDetails lookup.
    // Keeping it in case 'sub' (UUID) is needed elsewhere, but clearly mark its purpose.
    public String extractUsername(String token) {
        logger.warn("extractUsername (deprecated in favor of extractSubject or extractEmail) called. It extracts the 'sub' claim.");
        return extractSubject(token);
    }
}
