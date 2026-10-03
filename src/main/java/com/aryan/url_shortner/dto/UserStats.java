package com.aryan.url_shortner.dto;

public record UserStats(
        long totalUsers,
        long adminUsers,
        long standardUsers,
        long usersToday,
        long users7Days,
        long users30Days
) {}