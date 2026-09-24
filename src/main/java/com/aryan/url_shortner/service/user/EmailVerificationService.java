package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.repository.UserRepository;
import com.aryan.url_shortner.exceptions.InvalidVerificationTokenException;
import com.aryan.url_shortner.exceptions.EmailAlreadyVerifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements IEmailVerificationService{

    private final StringRedisTemplate redisTemplate;
    private final IEmailSender emailSender;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${api.version}")
    private String apiVersion;

    public void sendVerificationEmail(User user) {
        // Invalidate previous token if one exists (reverse lookup)
        String userKey = "verify_user:" + user.getId();
        String oldToken = redisTemplate.opsForValue().get(userKey);
        if (oldToken != null) {
            redisTemplate.delete("verify:" + oldToken);
        }

        // Generate cryptographically secure token
        String token = UUID.randomUUID().toString();

        // TTL 15 minutes
        redisTemplate.opsForValue().set("verify:" + token, user.getId().toString(), Duration.ofMinutes(15));
        redisTemplate.opsForValue().set(userKey, token, Duration.ofMinutes(15));

        String link = appBaseUrl + "/api/" + apiVersion + "/users/verify-email?token=" + token;
        emailSender.sendVerificationEmail(user.getEmail(), link);
    }

    public void verifyEmail(String token) {
        String tokenKey = "verify:" + token;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);

        if (userIdStr == null) {
            throw new InvalidVerificationTokenException("Verification link is invalid or has expired.");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidVerificationTokenException("User no longer exists."));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        // Cleanup Redis tokens
        redisTemplate.delete(tokenKey);
        redisTemplate.delete("verify_user:" + userId);
    }

    public void resendVerificationEmail(String email) {
        // Silent failure if user doesn't exist or is verified, preventing email enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendVerificationEmail(user);
            }
        });
    }
}