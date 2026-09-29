package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginResponse;
import com.aryan.url_shortner.exceptions.InvalidResetTokenException;
import com.aryan.url_shortner.exceptions.UserNotFoundException;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.CustomUserDetails;
import com.aryan.url_shortner.repository.UserRepository;
import com.aryan.url_shortner.service.security.IJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {

    private final UserRepository userRepository;
    private final IEmailSender emailSender;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private static final String RESET_TOKEN_PREFIX = "reset:";
    private static final String REVOKE_PREFIX = "user:jwt_revoked_before:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    @Override
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("No account found for this email. Please create an account.");
        }

        User user = userOpt.get();

        if (user.getAuthProvider() == com.aryan.url_shortner.enums.AuthProvider.GOOGLE) {
            return;
        }

        String token = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + token,
                user.getId().toString(),
                TOKEN_TTL
        );

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailSender.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    public LoginResponse resetPassword(String token, String newPassword) {
        String key = RESET_TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            throw new InvalidResetTokenException("Reset token is invalid or has expired.");
        }

        // Delete token to ensure single use (prevent race conditions)
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.FALSE.equals(deleted)) {
            throw new InvalidResetTokenException("Token already used.");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidResetTokenException("User no longer exists."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all existing JWTs for this user immediately
        redisTemplate.opsForValue().set(
                REVOKE_PREFIX + userId,
                String.valueOf(java.time.Instant.now().getEpochSecond()),
                Duration.ofDays(7) // Match your JWT max expiration time
        );

        String jwt = jwtService.generateToken(new CustomUserDetails(user));
        return new LoginResponse(user.getId(), user.getEmail(), jwt);
    }
}