package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.UrlStatus;

import java.time.Instant;
import java.util.UUID;

public record ShortenedUrlResponse(
    UUID id,
    String shortCode,
    String originalUrl,
    UrlStatus status,
    Long clickCount,
    Instant createdAt,
    Instant updatedAt
) {
}
