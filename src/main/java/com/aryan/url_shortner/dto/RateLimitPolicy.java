package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.RateLimitOperation;

import java.time.Duration;


public record RateLimitPolicy(
        RateLimitOperation operation,
        int capacity,
        Duration refillInterval
) {
}