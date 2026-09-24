package com.aryan.url_shortner.service.user;

import com.aryan.url_shortner.exceptions.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailSender implements IEmailSender {

    private final RestClient restClient;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public BrevoEmailSender() {
        this.restClient = RestClient.create();
    }

    @Override
    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "Verify your email for URLzs",
                "htmlContent", "<h2>Welcome to URLzs!</h2><p>Please verify your email by clicking the link below:</p><p><a href=\"" + verificationUrl + "\">Verify Email</a></p><p>This link will expire in 15 minutes.</p><p>If the button doesn't work, copy and paste this link: " + verificationUrl + "</p>"
        );

        try {
            restClient.post()
                    .uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (org.springframework.web.client.RestClientException e) {
            throw new EmailDeliveryException("Failed to send verification email. Please try again later.");
        }
    }
}