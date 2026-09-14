package com.aryan.url_shortner.dto;

import com.aryan.url_shortner.enums.UrlStatus;

public record UpdateUrlStatusRequest(
        UrlStatus status
) {}