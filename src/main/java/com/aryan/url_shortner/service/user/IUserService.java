package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginRequest;
import com.aryan.url_shortner.dto.RegisterUserRequest;
import com.aryan.url_shortner.model.User;

import java.util.UUID;

public interface IUserService {
    public User getUser(UUID id);
    public User getUserByEmail(String email);
    public boolean emailExists(String email);
    public User registerUser(RegisterUserRequest request);
    public User loginUser(LoginRequest request);
}
