package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.InfrastructureMetricsResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class InfrastructureAdminService implements IInfrastructureAdminService {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;

    @Override
    public InfrastructureMetricsResponse getInfrastructureMetrics() {
        return new InfrastructureMetricsResponse(
                getDatabaseMetrics(),
                getRedisMetrics()
        );
    }

    private InfrastructureMetricsResponse.DatabaseMetrics getDatabaseMetrics() {
        return new InfrastructureMetricsResponse.DatabaseMetrics(
                (int) getGaugeValue("hikaricp.connections.active"),
                (int) getGaugeValue("hikaricp.connections.idle"),
                (int) getGaugeValue("hikaricp.connections.pending"),
                (int) getGaugeValue("hikaricp.connections.max"),
                (int) getGaugeValue("hikaricp.connections.min")
        );
    }

    private InfrastructureMetricsResponse.RedisMetrics getRedisMetrics() {
        long usedMemory = 0;
        long connectedClients = 0;
        long totalCommands = 0;
        String status = "DOWN";

        try {
            assert redisTemplate.getConnectionFactory() != null;
            try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
                Properties info = connection.serverCommands().info();
                if (info != null) {
                    status = "UP";
                    usedMemory = parseLongProperty(info, "used_memory");
                    connectedClients = parseLongProperty(info, "connected_clients");
                    totalCommands = parseLongProperty(info, "total_commands_processed");
                }
            }
        } catch (Exception e) {
            status = "ERROR: " + e.getMessage();
        }

        long cacheHits = getCounterValue("redirect.cache.hits");
        long cacheMisses = getCounterValue("redirect.cache.misses");

        return new InfrastructureMetricsResponse.RedisMetrics(
                status,
                usedMemory,
                connectedClients,
                totalCommands,
                cacheHits,
                cacheMisses
        );
    }

    private double getGaugeValue(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private long getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? (long) counter.count() : 0L;
    }

    private long parseLongProperty(Properties info, String key) {
        try {
            return Long.parseLong(info.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}