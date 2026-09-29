package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginResponse;
import com.aryan.url_shortner.enums.AuthProvider;
import com.aryan.url_shortner.exceptions.InvalidGoogleTokenException;
import com.aryan.url_shortner.model.CustomUserDetails;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.repository.UserRepository;
import com.aryan.url_shortner.service.security.IJwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class GoogleAuthService implements IAuthService{

    private final UserRepository userRepository;
    private final IJwtService jwtService;

    @Value("${google.client.id}")
    private String googleClientId;

    @Override
    public LoginResponse authenticateGoogleUser(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        GoogleIdToken idToken;

        try {
            idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("Invalid Google ID token");
            }

        } catch (Exception e) {
            throw new InvalidGoogleTokenException("Failed to verify Google token: " + e.getMessage());
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        if (Boolean.FALSE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException("Google email is not verified");
        }

        String email = payload.getEmail();
        User user = findOrCreateUserSafely(email);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String jwt = jwtService.generateToken(userDetails);


        return new LoginResponse(user.getId(), user.getEmail(), jwt);
    }


    private User findOrCreateUserSafely(String email) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        try {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setEmailVerified(true);
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User creation race condition failed"));
        }
    }
}
