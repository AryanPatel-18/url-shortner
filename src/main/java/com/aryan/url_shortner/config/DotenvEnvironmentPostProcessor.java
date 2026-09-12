package com.aryan.url_shortner.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            @NonNull SpringApplication application) {

        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Map<String, Object> properties = new HashMap<>();

        properties.put("POSTGRES_HOST", dotenv.get("POSTGRES_HOST"));
        properties.put("POSTGRES_PORT", dotenv.get("POSTGRES_PORT"));
        properties.put("POSTGRES_DB", dotenv.get("POSTGRES_DB"));
        properties.put("POSTGRES_USER", dotenv.get("POSTGRES_USER"));
        properties.put("POSTGRES_PASSWORD", dotenv.get("POSTGRES_PASSWORD"));

        properties.put("REDIS_HOST", dotenv.get("REDIS_HOST"));
        properties.put("REDIS_PORT", dotenv.get("REDIS_PORT"));

        environment.getPropertySources().addLast(
                new MapPropertySource("dotenv", properties)
        );
    }
}