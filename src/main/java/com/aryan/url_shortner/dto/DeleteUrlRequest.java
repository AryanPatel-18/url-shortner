package com.aryan.url_shortner.dto;

import java.util.UUID;

public record DeleteUrlRequest(
        UUID urlId
) {}