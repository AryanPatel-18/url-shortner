package com.aryan.url_shortner.aspect;

import com.aryan.url_shortner.annotation.RateLimit;
import com.aryan.url_shortner.config.RateLimitProperties;
import com.aryan.url_shortner.dto.RateLimitPolicy;
import com.aryan.url_shortner.dto.RateLimitResult;
import com.aryan.url_shortner.exceptions.RateLimitExceededException;
import com.aryan.url_shortner.model.CustomUserDetails;
import com.aryan.url_shortner.service.rateLimit.IRateLimiter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    private final IRateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    @Before("@annotation(rateLimitAnnotation)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimitAnnotation) {
        String key;
        boolean isUserId = false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            key = userDetails.getUser().getId().toString();
            isUserId = true;
        } else {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null) {
                    key = request.getRemoteAddr();
                } else {
                    key = xfHeader.split(",")[0];
                }
            } else {
                key = "anonymous";
            }
        }

        RateLimitProperties.PolicyConfig config = rateLimitProperties.getPolicyForOperation(rateLimitAnnotation.operation());

        RateLimitPolicy policy = new RateLimitPolicy(
                rateLimitAnnotation.operation(),
                config.getCapacity(),
                Duration.ofSeconds(config.getRefillInterval())
        );

        RateLimitResult result;
        if (isUserId) {
            result = rateLimiter.check(UUID.fromString(key), policy);
        } else {
            result = rateLimiter.check(key, policy);
        }

        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
    }
}
