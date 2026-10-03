package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.SystemMetricsResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SystemAdminService implements ISystemAdminService {

    private final MeterRegistry meterRegistry;

    @Override
    public SystemMetricsResponse getSystemMetrics() {
        return new SystemMetricsResponse(
                getApplicationMetrics(),
                getHttpMetrics(),
                getJvmMetrics(),
                getCpuMetrics()
        );
    }

    private SystemMetricsResponse.ApplicationMetrics getApplicationMetrics() {
        return new SystemMetricsResponse.ApplicationMetrics(
                "UP",
                getGaugeValue("process.uptime"),
                getGaugeValue("process.start.time")
        );
    }

    private SystemMetricsResponse.HttpMetrics getHttpMetrics() {
        long totalRequests = 0;
        double totalTimeMs = 0;
        double maxLatencyMs = 0;

        for (Timer timer : meterRegistry.find("http.server.requests").timers()) {
            totalRequests += timer.count();
            totalTimeMs += timer.totalTime(TimeUnit.MILLISECONDS);
            maxLatencyMs = Math.max(maxLatencyMs, timer.max(TimeUnit.MILLISECONDS));
        }

        double avgLatencyMs = totalRequests > 0 ? (totalTimeMs / totalRequests) : 0.0;

        return new SystemMetricsResponse.HttpMetrics(
                totalRequests,
                getOutcomeCount("SUCCESS"),
                getOutcomeCount("REDIRECTION"),
                getOutcomeCount("CLIENT_ERROR"),
                getOutcomeCount("SERVER_ERROR"),
                avgLatencyMs,
                maxLatencyMs
        );
    }

    private SystemMetricsResponse.JvmMetrics getJvmMetrics() {
        return new SystemMetricsResponse.JvmMetrics(
                getGaugeValue("jvm.memory.used", "heap") / 1_048_576.0,
                getGaugeValue("jvm.memory.committed", "heap") / 1_048_576.0,
                getGaugeValue("jvm.memory.max", "heap") / 1_048_576.0,
                getGaugeValue("jvm.memory.used", "nonheap") / 1_048_576.0,
                (int) getGaugeValue("jvm.threads.live"),
                (int) getGaugeValue("jvm.threads.peak"),
                (int) getGaugeValue("jvm.threads.daemon")
        );
    }

    private SystemMetricsResponse.CpuMetrics getCpuMetrics() {
        return new SystemMetricsResponse.CpuMetrics(
                getGaugeValue("system.cpu.usage"),
                getGaugeValue("process.cpu.usage")
        );
    }

    private double getGaugeValue(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private double getGaugeValue(String name, String tagValue) {
        Gauge gauge = meterRegistry.find(name).tag("area", tagValue).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private long getOutcomeCount(String outcome) {
        long count = 0;
        for (Timer timer : meterRegistry.find("http.server.requests").tag("outcome", outcome).timers()) {
            count += timer.count();
        }
        return count;
    }
}