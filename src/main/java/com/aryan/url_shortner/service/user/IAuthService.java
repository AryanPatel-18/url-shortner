package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginResponse;

public interface IAuthService {
    LoginResponse authenticateGoogleUser(String idTokenString);
}
