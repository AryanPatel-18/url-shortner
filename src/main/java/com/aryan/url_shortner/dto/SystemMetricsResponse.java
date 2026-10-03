package com.aryan.url_shortner.dto;

public record SystemMetricsResponse(
        ApplicationMetrics application,
        HttpMetrics http,
        JvmMetrics jvm,
        CpuMetrics cpu
) {
    public record ApplicationMetrics(
            String status,
            double uptimeSeconds,
            double startTime
    ) {}

    public record HttpMetrics(
            long totalRequests,
            long requests2xx,
            long requests3xx,
            long requests4xx,
            long requests5xx,
            double avgLatencyMs,
            double maxLatencyMs
    ) {}

    public record JvmMetrics(
            double heapUsedMb,
            double heapCommittedMb,
            double heapMaxMb,
            double nonHeapUsedMb,
            int liveThreads,
            int peakThreads,
            int daemonThreads
    ) {}

    public record CpuMetrics(
            double systemCpuUsage,
            double processCpuUsage
    ) {}
}