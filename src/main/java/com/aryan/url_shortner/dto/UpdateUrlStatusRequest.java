package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.UrlStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUrlStatusRequest(

        @NotNull(message = "Status is required. Allowed values: ACTIVE, DISABLED")
        UrlStatus status
) {
}