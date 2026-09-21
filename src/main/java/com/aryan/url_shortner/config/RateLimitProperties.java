package com.aryan.url_shortner.config;

import com.aryan.url_shortner.enums.RateLimitOperation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;


@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    private Map<String, PolicyConfig> policies;
    @Getter
    @Setter
    public static class PolicyConfig {
        private int capacity;
        private long refillInterval;
    }
    public PolicyConfig getPolicyForOperation(RateLimitOperation operation) {
        return policies.get(operation.name().toLowerCase());
    }
}