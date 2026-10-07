package com.aryan.url_shortner.dto;

import java.time.Instant;

public record AdminDashboardResponse(
        ApplicationSection application,
        HttpSection http,
        DatabaseSection database,
        RedisSection redis,
        JvmSection jvm,
        BusinessSection business,
        WorkerSection workers
) {
    public record ApplicationSection(
            String uptime,
            Instant startTime,
            String status
    ) {}

    public record HttpSection(
            long totalRequests,
            long successfulRequests,
            long clientErrors,
            long serverErrors,
            long rateLimitedRequests,
            double p50LatencyMs,
            double p95LatencyMs,
            double p99LatencyMs
    ) {}

    public record DatabaseSection(
            int activeConnections,
            int idleConnections,
            int pendingConnections,
            int maxConnections,
            int minConnections,
            long connectionTimeouts
    ) {}

    public record RedisSection(
            String status,
            long connectedClients,
            long totalCommandsProcessed,
            long memoryUsedBytes,
            long memoryMaxBytes,
            long cacheHits,
            long cacheMisses
    ) {}

    public record JvmSection(
            long heapUsedBytes,
            long heapMaxBytes,
            long nonHeapUsedBytes,
            int liveThreads,
            double cpuUsage,
            long gcPauseCount,
            double gcPauseTotalMs
    ) {}

    public record BusinessSection(
            long totalUsers,
            long adminUsers,
            long standardUsers,
            long totalUrls,
            long activeUrls,
            long disabledUrls,
            long totalClicks
    ) {}

    public record WorkerSection(
            long clickFlushExecutions,
            long clickFlushFailures,
            double lastFlushDurationMs
    ) {}
}
