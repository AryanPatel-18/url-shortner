package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.model.User;

import java.util.UUID;

public interface IUserService {
    public User getUser(UUID id);
    public User getUserByEmail(String email);
    public boolean emailExists(String email);
}
