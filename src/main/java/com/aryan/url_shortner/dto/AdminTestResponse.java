package com.aryan.url_shortner.dto;

import java.time.LocalDateTime;

public record AdminTestResponse(
        String message,
        LocalDateTime timestamp
) {}