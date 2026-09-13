package com.aryan.url_shortner.dto;

import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String originalUrl,
        String shortCode
) {}
