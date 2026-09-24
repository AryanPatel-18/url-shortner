package com.aryan.url_shortner.service.rateLimit;

import com.aryan.url_shortner.dto.RateLimitPolicy;
import com.aryan.url_shortner.dto.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimiterService implements IRateLimiter{

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimiterScript = new DefaultRedisScript<>();

    {
        rateLimiterScript.setScriptSource(
                new ResourceScriptSource(
                        new ClassPathResource("scripts/rate_limiter.lua")
                )
        );
        rateLimiterScript.setResultType(List.class);
    }

    @Override
    public RateLimitResult check(UUID userId, RateLimitPolicy policy) {
        String key = "rate_limit:user:" + userId + ":" + policy.operation().name().toLowerCase();
        return executeCheck(key, policy);
    }

    @Override
    public RateLimitResult check(String keyIdentifier, RateLimitPolicy policy) {
        String key = "rate_limit:custom:" + keyIdentifier + ":" + policy.operation().name().toLowerCase();
        return executeCheck(key, policy);
    }

    private RateLimitResult executeCheck(String key, RateLimitPolicy policy) {
        long now = System.currentTimeMillis();
        long refillInterval = policy.refillInterval().toMillis();
        long ttl = calculateTtl(policy).toSeconds();

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = (List<Long>) redisTemplate.execute(
                    rateLimiterScript,
                    List.of(key),
                    String.valueOf(policy.capacity()),
                    String.valueOf(refillInterval),
                    String.valueOf(now),
                    String.valueOf(ttl)
            );

            if (result.size() < 3) {
                return new RateLimitResult(true, -1, 0);
            }

            boolean allowed = result.get(0) == 1;
            long remainingTokens = result.get(1);
            long retryAfterSeconds = result.get(2);

            return new RateLimitResult(allowed, remainingTokens, retryAfterSeconds);
        } catch (Exception e) {
            System.err.println("Rate limiter failed, failing open: " + e.getMessage());
            return new RateLimitResult(true, -1, 0);
        }
    }

    private Duration calculateTtl(RateLimitPolicy policy) {
        return policy.refillInterval().multipliedBy(policy.capacity());
    }
}
