package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.UrlStatus;

import java.util.UUID;

public record RedirectCacheDTO(
        UUID id,
        String originalUrl,
        UrlStatus status
) {}
