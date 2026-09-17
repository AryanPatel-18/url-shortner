package com.aryan.url_shortner.service.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IJwtService jwtService;
    private final ICustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException, java.io.IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            // If the token is invalid or expired, this will throw an exception
            if (jwtService.isTokenValid(token)) {

                // 1. Extract data directly from the token (NO DATABASE LOOKUP)
                String email = jwtService.extractUsername(token);
                java.util.UUID userId = jwtService.extractUserId(token);
                // 2. Reconstruct a lightweight User object in memory
                UsernamePasswordAuthenticationToken authentication = getAuthentication(userId, email);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                throw new JwtException("Token is expired or invalid");
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "status": 401,
                    "message": "Invalid or expired JWT token"
                }
            """);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static @NonNull UsernamePasswordAuthenticationToken getAuthentication(UUID userId, String email) {
        com.aryan.url_shortner.model.User user = new com.aryan.url_shortner.model.User();
        user.setId(userId);
        user.setEmail(email);
        // 3. Create the CustomUserDetails directly
        com.aryan.url_shortner.model.CustomUserDetails userDetails =
                new com.aryan.url_shortner.model.CustomUserDetails(user);
        // 4. Authenticate the user
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}
