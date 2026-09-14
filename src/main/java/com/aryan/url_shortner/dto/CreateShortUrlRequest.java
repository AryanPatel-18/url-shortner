package com.aryan.url_shortner.dto;

import java.util.UUID;

public record CreateShortUrlRequest(
        String originalUrl
) {}