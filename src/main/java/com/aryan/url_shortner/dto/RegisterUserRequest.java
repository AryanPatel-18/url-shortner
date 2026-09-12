package com.aryan.url_shortner.dto;


public record RegisterUserRequest(
        String email,
        String password
) {
}