package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.dto.LoginResponse;

public interface IPasswordResetService {
    void requestPasswordReset(String email);
    LoginResponse resetPassword(String token, String newPassword);
}