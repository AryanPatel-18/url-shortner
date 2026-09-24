package com.aryan.url_shortner.controller;

import com.aryan.url_shortner.annotation.RateLimit;
import com.aryan.url_shortner.dto.*;
import com.aryan.url_shortner.enums.RateLimitOperation;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.service.user.EmailVerificationService;
import com.aryan.url_shortner.service.user.IEmailVerificationService;
import com.aryan.url_shortner.service.user.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/${api.version}/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final IEmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UserResponse(
                        user.getId(),
                        user.getEmail()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.loginUser(request));
    }

    @org.springframework.beans.factory.annotation.Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyEmail(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(frontendUrl + "/login?verified=true"))
                    .build();
        } catch (com.aryan.url_shortner.exceptions.InvalidVerificationTokenException | 
                 com.aryan.url_shortner.exceptions.EmailAlreadyVerifiedException ex) {
            String encodedError = java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(java.net.URI.create(frontendUrl + "/login?error=" + encodedError))
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    @RateLimit(operation = RateLimitOperation.RESEND_VERIFICATION)
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.email());

        return ResponseEntity.ok(Map.of("message", "If the email is registered and unverified, a new link has been sent."));
    }

    @GetMapping("/check-verification")
    public ResponseEntity<Map<String, Boolean>> checkVerification(@RequestParam String email) {
        boolean verified = userService.isEmailVerified(email);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(org.springframework.security.core.Authentication authentication) {
        com.aryan.url_shortner.model.CustomUserDetails userDetails = 
                (com.aryan.url_shortner.model.CustomUserDetails) authentication.getPrincipal();
        userService.deleteUser(userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}