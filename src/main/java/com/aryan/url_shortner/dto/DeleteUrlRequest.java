package com.aryan.url_shortner.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeleteUrlRequest(

        @NotNull(message = "URL ID is required")
        UUID urlId
) {
}