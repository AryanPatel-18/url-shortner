package com.aryan.url_shortner.service.security;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public interface IJwtService {
    String generateToken(UserDetails userDetails);
    String extractUsername(String token);
    UUID extractUserId(String token);
    boolean isTokenValid(String token);
}