package com.aryan.url_shortner.service.rateLimit;

import com.aryan.url_shortner.dto.RateLimitPolicy;
import com.aryan.url_shortner.dto.RateLimitResult;

import java.util.UUID;

public interface IRateLimiter {
    RateLimitResult check(UUID userId, RateLimitPolicy policy);
    RateLimitResult check(String keyIdentifier, RateLimitPolicy policy);
}
