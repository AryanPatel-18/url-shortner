package com.aryan.url_shortner.dto;

import java.util.List;

public record UserUrlsResponse(
        List<UserUrlResponse> urls
) {}
