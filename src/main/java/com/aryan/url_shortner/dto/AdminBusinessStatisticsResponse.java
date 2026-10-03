package com.aryan.url_shortner.dto;

public record AdminBusinessStatisticsResponse(
        UserStatistics users,
        UrlStatistics urls,
        ClickStatistics clicks
) {
    public record UserStatistics(
            long total,
            long admins,
            long standardUsers,
            long registeredToday,
            long registeredLast7Days,
            long registeredLast30Days
    ) {}

    public record UrlStatistics(
            long total,
            long active,
            long disabled,
            long createdToday,
            long createdLast7Days,
            long createdLast30Days
    ) {}

    public record ClickStatistics(
            long total,
            String note
    ) {}
}