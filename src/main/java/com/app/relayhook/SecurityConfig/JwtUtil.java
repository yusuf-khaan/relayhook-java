package com.app.relayhook.SecurityConfig;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

    // @SuppressWarnings("deprecation")
    // private final SecretKey secretKey =
    // Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private final SecretKey secretKey;
    private final SecretKey secretKeyTwo;
    private static final long EXPIRATION_MILLIS = 1000L * 60 * 60 * 24 * 7; // 7 days

    public JwtUtil(@Value("${spring.jwt.secret}") String secret, @Value("${spring.jwt.secret.two}") String secretTwo) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters long");
        }
        if (secretTwo == null || secretTwo.length() < 32) {
            throw new IllegalArgumentException("JWT secret two must be at least 32 characters long");
        }
        // logger.info("JWT Secret Key Length: {}", secret.length());
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.secretKeyTwo = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MILLIS))
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            System.out.println("JWT Token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("JWT Token is unsupported: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("JWT Token is malformed: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("JWT Signature validation failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("JWT Token is missing or invalid: " + e.getMessage());
        }
        return false;
    }

    public Claims parseAllClaims(String token) { // authenticate adn extract detail of jwt
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateLocalStorageTokens(String email, Map<String, String> customClaims) {
        log.info("email is  " + email);
        log.info("customClaims is  " + customClaims);
        JwtBuilder builder = Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 60)) // 1 hour
                .claims(customClaims);

        if (email != null && !email.isBlank()) {
            builder.subject(email);
        }

        return builder.signWith(secretKeyTwo).compact();
    }

    public Claims parseAllClaimsForSecretKeyTwo(String token) { // authenticate adn extract detail of jwt
        return Jwts.parser()
                .verifyWith(secretKeyTwo)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
