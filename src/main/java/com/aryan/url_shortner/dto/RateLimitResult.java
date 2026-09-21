package com.aryan.url_shortner.dto;

public record RateLimitResult(
        boolean allowed,
        long remainingTokens,
        long retryAfterSeconds
) {
}
