package com.aryan.url_shortner.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email
) {
}
