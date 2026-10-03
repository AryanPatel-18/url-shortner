package com.aryan.url_shortner.dto;

public record InfrastructureMetricsResponse(
        DatabaseMetrics database,
        RedisMetrics redis
) {
    public record DatabaseMetrics(
            int activeConnections,
            int idleConnections,
            int pendingConnections,
            int maxPoolSize,
            int minIdleConnections
    ) {}

    public record RedisMetrics(
            String status,
            long usedMemoryBytes,
            long connectedClients,
            long totalCommandsProcessed,
            long redirectCacheHits,
            long redirectCacheMisses
    ) {}
}