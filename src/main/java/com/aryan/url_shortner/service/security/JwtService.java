package com.aryan.url_shortner.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService implements IJwtService{

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public String generateToken(UserDetails userDetails) {
        // Cast to our custom implementation to get access to the User object
        com.aryan.url_shortner.model.CustomUserDetails customUser =
                (com.aryan.url_shortner.model.CustomUserDetails) userDetails;
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", customUser.getUser().getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        String username = extractAllClaims(token).getSubject();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username not found in JWT");
        }

        return username;
    }

    @Override
    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new IllegalArgumentException("User ID not found in JWT");
        }
        return UUID.fromString(userIdStr);
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private     SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
