package com.omraty.backend.config.security;

import com.omraty.backend.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_PHONE = "phone";
    private static final String CLAIM_TYPE = "type";

    private enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String value;

        TokenType(String value) {
            this.value = value;
        }
    }

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.accessTokenExpirationMinutes}") long accessTokenExpirationMinutes,
            @Value("${jwt.refreshTokenExpirationDays}") long refreshTokenExpirationDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String generateAccessToken(UUID userId, String phone) {
        return generateToken(
                userId, phone, TokenType.ACCESS, Duration.ofMinutes(accessTokenExpirationMinutes));
    }

    public String generateRefreshToken(UUID userId, String phone) {
        return generateToken(
                userId, phone, TokenType.REFRESH, Duration.ofDays(refreshTokenExpirationDays));
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException.InvalidTokenException("Invalid or expired JWT token", e);
        }
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public boolean isAccessToken(String token) {
        return TokenType.ACCESS.value.equals(validateToken(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TokenType.REFRESH.value.equals(validateToken(token).get(CLAIM_TYPE, String.class));
    }

    public Instant getExpiration(String token) {
        return validateToken(token).getExpiration().toInstant();
    }

    private String generateToken(UUID userId, String phone, TokenType type, Duration expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_PHONE, phone)
                .claim(CLAIM_TYPE, type.value)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}
