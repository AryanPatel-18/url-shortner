package com.aryan.url_shortner.controller;


import com.aryan.url_shortner.dto.LoginRequest;
import com.aryan.url_shortner.dto.LoginResponse;
import com.aryan.url_shortner.dto.RegisterUserRequest;
import com.aryan.url_shortner.dto.UserResponse;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.service.user.IUserService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${api.version}/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping("/register")
    public UserResponse registerUser(@RequestBody RegisterUserRequest request) {
        User user =  userService.registerUser(request);
        return new UserResponse(
                user.getId(),
                user.getEmail()
        );
    }

    @PostMapping("/login")
    public LoginResponse loginUser(@RequestBody LoginRequest request) {
        return userService.loginUser(request);
    }
}