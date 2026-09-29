package com.aryan.url_shortner.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "ID Token is required")
        String idToken
) {}