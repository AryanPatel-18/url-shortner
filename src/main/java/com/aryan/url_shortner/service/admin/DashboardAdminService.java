package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.AdminDashboardResponse;
import com.aryan.url_shortner.dto.UrlStats;
import com.aryan.url_shortner.dto.UserStats;
import com.aryan.url_shortner.enums.Role;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import com.aryan.url_shortner.repository.UserRepository;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DashboardAdminService implements IDashboardAdminService {

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ShortenedUrlRepository shortenedUrlRepository;

    @Override
    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                buildApplication(),
                buildHttp(),
                buildDatabase(),
                buildRedis(),
                buildJvm(),
                buildBusiness(),
                buildWorkers()
        );
    }

    // ── Application ──────────────────────────────────────────────

    private AdminDashboardResponse.ApplicationSection buildApplication() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Instant startTime = Instant.ofEpochMilli(
                ManagementFactory.getRuntimeMXBean().getStartTime()
        );
        return new AdminDashboardResponse.ApplicationSection(
                Duration.ofMillis(uptimeMs).toString(),
                startTime,
                "UP"
        );
    }

    // ── HTTP ─────────────────────────────────────────────────────

    private AdminDashboardResponse.HttpSection buildHttp() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();

        long totalRequests = 0;
        long successfulRequests = 0;
        long clientErrors = 0;
        long serverErrors = 0;
        long rateLimitedRequests = 0;

        for (Timer timer : timers) {
            long count = timer.count();
            totalRequests += count;

            String status = timer.getId().getTag("status");
            if (status == null) continue;

            int code;
            try {
                code = Integer.parseInt(status);
            } catch (NumberFormatException e) {
                continue;
            }

            if (code >= 200 && code < 400) {
                successfulRequests += count;
            } else if (code == 429) {
                rateLimitedRequests += count;
                clientErrors += count;
            } else if (code >= 400 && code < 500) {
                clientErrors += count;
            } else if (code >= 500) {
                serverErrors += count;
            }
        }

        return new AdminDashboardResponse.HttpSection(
                totalRequests,
                successfulRequests,
                clientErrors,
                serverErrors,
                rateLimitedRequests,
                getPercentile("http.server.requests", 0.5),
                getPercentile("http.server.requests", 0.95),
                getPercentile("http.server.requests", 0.99)
        );
    }

    // ── Database (HikariCP) ──────────────────────────────────────

    private AdminDashboardResponse.DatabaseSection buildDatabase() {
        return new AdminDashboardResponse.DatabaseSection(
                (int) gaugeValue("hikaricp.connections.active"),
                (int) gaugeValue("hikaricp.connections.idle"),
                (int) gaugeValue("hikaricp.connections.pending"),
                (int) gaugeValue("hikaricp.connections.max"),
                (int) gaugeValue("hikaricp.connections.min"),
                counterValue("hikaricp.connections.timeout")
        );
    }

    // ── Redis ────────────────────────────────────────────────────

    private AdminDashboardResponse.RedisSection buildRedis() {
        long usedMemory = 0;
        long maxMemory = 0;
        long connectedClients = 0;
        long totalCommands = 0;
        String status = "DOWN";

        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            try {
                Properties info = connection.serverCommands().info();
                if (info != null) {
                    status = "UP";
                    usedMemory = parseLong(info, "used_memory");
                    maxMemory = parseLong(info, "maxmemory");
                    connectedClients = parseLong(info, "connected_clients");
                    totalCommands = parseLong(info, "total_commands_processed");
                }
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            status = "ERROR: " + e.getMessage();
        }

        return new AdminDashboardResponse.RedisSection(
                status,
                connectedClients,
                totalCommands,
                usedMemory,
                maxMemory,
                counterValue("redirect.cache.hits"),
                counterValue("redirect.cache.misses")
        );
    }

    // ── JVM ──────────────────────────────────────────────────────

    private AdminDashboardResponse.JvmSection buildJvm() {
        return new AdminDashboardResponse.JvmSection(
                (long) gaugeValue("jvm.memory.used", "area", "heap"),
                (long) gaugeValue("jvm.memory.max", "area", "heap"),
                (long) gaugeValue("jvm.memory.used", "area", "nonheap"),
                (int) gaugeValue("jvm.threads.live"),
                gaugeValue("process.cpu.usage"),
                timerCount("jvm.gc.pause"),
                timerTotal("jvm.gc.pause")
        );
    }

    // ── Business ─────────────────────────────────────────────────

    private AdminDashboardResponse.BusinessSection buildBusiness() {
        Instant now = Instant.now();
        UserStats userStats = userRepository.getUserStatistics(
                Role.ADMIN, Role.USER,
                now.minus(1, ChronoUnit.DAYS),
                now.minus(7, ChronoUnit.DAYS),
                now.minus(30, ChronoUnit.DAYS)
        );

        LocalDateTime localNow = LocalDateTime.now();
        UrlStats urlStats = shortenedUrlRepository.getUrlStatistics(
                UrlStatus.ACTIVE, UrlStatus.DISABLED,
                localNow.minusDays(1),
                localNow.minusDays(7),
                localNow.minusDays(30)
        );

        long totalClicks = shortenedUrlRepository.sumTotalClicks();

        return new AdminDashboardResponse.BusinessSection(
                userStats.totalUsers(),
                userStats.adminUsers(),
                userStats.standardUsers(),
                urlStats.totalUrls(),
                urlStats.activeUrls(),
                urlStats.disabledUrls(),
                totalClicks
        );
    }

    // ── Workers ──────────────────────────────────────────────────

    private AdminDashboardResponse.WorkerSection buildWorkers() {
        long executions = 0;
        long failures = 0;
        double lastDurationMs = 0;

        Timer successTimer = meterRegistry.find("tasks.scheduled.execution")
                .tag("outcome", "SUCCESS")
                .timer();
        Timer errorTimer = meterRegistry.find("tasks.scheduled.execution")
                .tag("outcome", "ERROR")
                .timer();

        if (successTimer != null) {
            executions += successTimer.count();
            lastDurationMs = successTimer.max(TimeUnit.MILLISECONDS);
        }
        if (errorTimer != null) {
            failures = errorTimer.count();
            executions += failures;
        }

        return new AdminDashboardResponse.WorkerSection(
                executions,
                failures,
                lastDurationMs
        );
    }

    // ── Helpers ──────────────────────────────────────────────────

    private double gaugeValue(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private double gaugeValue(String name, String tagKey, String tagValue) {
        Gauge gauge = meterRegistry.find(name).tag(tagKey, tagValue).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }

    private long counterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? (long) counter.count() : 0L;
    }

    private long timerCount(String name) {
        Timer timer = meterRegistry.find(name).timer();
        return timer != null ? timer.count() : 0L;
    }

    private double timerTotal(String name) {
        Timer timer = meterRegistry.find(name).timer();
        return timer != null ? timer.totalTime(TimeUnit.MILLISECONDS) : 0.0;
    }

    private double getPercentile(String name, double percentile) {
        Timer timer = meterRegistry.find(name).timer();
        if (timer == null) return 0.0;
        return timer.percentile(percentile, TimeUnit.MILLISECONDS);
    }

    private long parseLong(Properties props, String key) {
        try {
            return Long.parseLong(props.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
