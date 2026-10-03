package com.aryan.url_shortner.service.security;

import com.aryan.url_shortner.enums.Role;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IJwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            if (jwtService.isTokenValid(token)) {

                UUID userId = jwtService.extractUserId(token);
                Date iat = jwtService.extractIssuedAt(token);
                String roleStr = jwtService.extractRole(token);
                Role role;

                if (roleStr == null || roleStr.isBlank()) {
                    role = Role.USER;
                } else {
                    try {
                        role = Role.valueOf(roleStr);
                    } catch (IllegalArgumentException e) {
                        role = Role.USER;
                    }
                }

                String revokedTimestampStr = redisTemplate.opsForValue().get("user:jwt_revoked_before:" + userId);
                if (revokedTimestampStr != null) {
                    long revokedSeconds = Long.parseLong(revokedTimestampStr);
                    if (iat.getTime() / 1000 < revokedSeconds) {
                        throw new JwtException("Token was revoked due to a password change");
                    }
                }

                String email = jwtService.extractUsername(token);
                UsernamePasswordAuthenticationToken authentication = getAuthentication(userId, email, role);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                throw new JwtException("Token is expired or invalid");
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, "Invalid or expired JWT token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":401,\"message\":\"" + message + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}"
        );
    }

    private static @NonNull UsernamePasswordAuthenticationToken getAuthentication(UUID userId, String email, Role role) {
        com.aryan.url_shortner.model.User user = new com.aryan.url_shortner.model.User();

        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);

        com.aryan.url_shortner.model.CustomUserDetails userDetails =
                new com.aryan.url_shortner.model.CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}