package com.aryan.url_shortner.dto;

import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String email,
        String token
) {}
