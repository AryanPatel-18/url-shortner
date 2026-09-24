package com.aryan.url_shortner.service.user;

public interface IEmailSender {
    /**
     * Sends an email verification link to the specified user.
     *
     * @param toEmail The recipient's email address
     * @param verificationUrl The complete URL for verification
     * @throws com.aryan.url_shortner.exceptions.EmailDeliveryException if delivery fails
     */
    void sendVerificationEmail(String toEmail, String verificationUrl);
}