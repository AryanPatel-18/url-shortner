package com.aryan.url_shortner.dto;

public record RegisterRequest(
        String email,
        String password
) {
}