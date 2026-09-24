package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginRequest;
import com.aryan.url_shortner.dto.LoginResponse;
import com.aryan.url_shortner.dto.RegisterUserRequest;
import com.aryan.url_shortner.exceptions.InvalidCredentialsException;
import com.aryan.url_shortner.exceptions.UserAlreadyExistsException;
import com.aryan.url_shortner.exceptions.UserNotFoundException;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.repository.UserRepository;
import com.aryan.url_shortner.model.CustomUserDetails;
import com.aryan.url_shortner.service.security.IJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final IJwtService jwtService;
    private final IEmailVerificationService emailVerificationService;
    private final com.aryan.url_shortner.repository.UserUrlRepository userUrlRepository;

    @Override
    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(()->new UserNotFoundException("The user does not exist"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("This user does not exist"));
    }
    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    @Override
    @jakarta.transaction.Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("The user does not exist");
        }
        userUrlRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    @Override
    public User registerUser(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        emailVerificationService.sendVerificationEmail(savedUser);
        
        return savedUser;
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new com.aryan.url_shortner.exceptions.EmailNotVerifiedException("Please verify your email address to log in.");
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assert userDetails != null;
        User user = userDetails.getUser();
        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(user.getId(), user.getEmail(), token);
    }



}
