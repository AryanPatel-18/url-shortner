package com.aryan.url_shortner.dto;

public record UrlStats(
        long totalUrls,
        long activeUrls,
        long disabledUrls,
        long urlsToday,
        long urls7Days,
        long urls30Days
) {}