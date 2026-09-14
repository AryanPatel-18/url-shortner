package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.UrlStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserUrlResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        UrlStatus status,
        long clickCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
