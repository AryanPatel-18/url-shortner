package com.aryan.url_shortner.service.url;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClickCountFlushWorker {

    private static final String ACTIVE_KEY = "url:clicks:active";
    private static final String FLUSH_KEY_PREFIX = "url:clicks:flush:";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 5000)
    public void flushClickCounts() {

        if (!Boolean.TRUE.equals(redisTemplate.hasKey(ACTIVE_KEY))) {
            return;
        }

        String flushKey =
                FLUSH_KEY_PREFIX + UUID.randomUUID();

        try {
            redisTemplate.rename(ACTIVE_KEY, flushKey);

            Map<Object, Object> clickCounts =
                    redisTemplate.opsForHash().entries(flushKey);

            if (clickCounts.isEmpty()) {
                redisTemplate.delete(flushKey);
                return;
            }

            jdbcTemplate.batchUpdate(
                    """
                        UPDATE shortened_urls
                        SET click_count = click_count + ?
                        WHERE id = ?
                    """,
                    clickCounts.entrySet(),
                    clickCounts.size(),
                    (ps, entry) -> {
                        ps.setLong(
                                1,
                                Long.parseLong(entry.getValue().toString())
                        );

                        ps.setObject(
                                2,
                                UUID.fromString(entry.getKey().toString())
                        );
                    }
            );

            redisTemplate.delete(flushKey);

        } catch (Exception e) {
            System.err.println(
                    "Failed to flush click counts: " + e.getMessage()
            );
        }
    }
}
