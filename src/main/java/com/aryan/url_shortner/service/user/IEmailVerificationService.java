package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.model.User;

public interface IEmailVerificationService {
    public void sendVerificationEmail(User user);
    public void verifyEmail(String token);
    public void resendVerificationEmail(String email);
}
