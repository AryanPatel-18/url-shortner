package com.aryan.url_shortner.service.userUrl;

import com.aryan.url_shortner.dto.UserUrlResponse;
import com.aryan.url_shortner.dto.UserUrlsResponse;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.exceptions.UserUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.repository.UserRepository;
import com.aryan.url_shortner.repository.UserUrlRepository;
import com.aryan.url_shortner.service.url.IShortenedUrlService;
import com.aryan.url_shortner.service.user.IUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserUrlService implements IUserUrlService{

    private final UserUrlRepository userUrlRepository;
    private final UserRepository userRepository;
    private final IUserService userService;
    private final IShortenedUrlService shortenedUrlService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public UserUrl addUrlToUser(UUID userId, ShortenedUrl shortenedUrl) {
        Optional<UserUrl> existing = userUrlRepository
                .findByUserIdAndShortenedUrlId(
                        userId,
                        shortenedUrl.getId()
                );
        if (existing.isPresent()) {
            UserUrl userUrl = existing.get();
            userUrl.setShortenedUrl(shortenedUrl);
            return userUrl;
        }

        User user = userRepository.getReferenceById(userId);
        UserUrl userUrl = new UserUrl();
        userUrl.setUser(user);
        userUrl.setShortenedUrl(shortenedUrl);
        clearUserUrlsCache(userId);
        return userUrlRepository.save(userUrl);
    }

    @Override
    public UserUrlsResponse getUserUrls(UUID userId, int page, int size) {

        long cacheVersion = getCacheVersion(userId);
        String key = "user:urls:" + userId + ":v:" + cacheVersion + ":page:" + page + ":size:" + size;

        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                return objectMapper.readValue(
                        cachedValue,
                        UserUrlsResponse.class
                );
            } catch (JacksonException ignored) {
                // Invalid cache entry, fall back to database
            }
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<UserUrlResponse> urls = userUrlRepository
                .findByUserIdWithShortenedUrl(userId, pageable)
                .stream()
                .map(userUrl -> new UserUrlResponse(
                        userUrl.getId(),
                        userUrl.getShortenedUrl().getOriginalUrl(),
                        userUrl.getShortenedUrl().getShortCode(),
                        userUrl.getShortenedUrl().getStatus(),
                        userUrl.getShortenedUrl().getClickCount(),
                        userUrl.getCreatedAt(),
                        userUrl.getUpdatedAt()
                ))
                .toList();

        UserUrlsResponse response = new UserUrlsResponse(urls);

        try {
            String cacheData = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(
                    key,
                    cacheData,
                    Duration.ofMinutes(2)
            );
        } catch (JacksonException ignored) {
            // Cache failure should not affect the response
        }

        return response;
    }

    @Transactional
    @Override
    public UserUrl updateStatus(UUID userId, UUID urlId, UrlStatus status) {
        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("User URL not found"));

        userUrl.getShortenedUrl().setStatus(status);
        shortenedUrlService.invalidateRedirectCache(userUrl.getShortenedUrl().getShortCode());
        clearUserUrlsCache(userId);
        return userUrlRepository.save(userUrl);
    }

    @Override
    public void removeUserUrl(UUID userId, UUID urlId) {

        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("URL not found for user"));
        clearUserUrlsCache(userId);
        userUrlRepository.delete(userUrl);
    }

    @Override
    public UserUrl createUserUrl(UUID userId, String originalUrl) {
        ShortenedUrl shortenedUrl =
                shortenedUrlService.getOrCreateShortenedUrl(originalUrl);
        return addUrlToUser(userId, shortenedUrl);
    }

    @Override
    public UserUrlResponse getUserUrl(UUID userId, UUID urlId) {

        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("URL not found for user"));

        return new UserUrlResponse(
                userUrl.getId(),
                userUrl.getShortenedUrl().getOriginalUrl(),
                userUrl.getShortenedUrl().getShortCode(),
                userUrl.getShortenedUrl().getStatus(),
                userUrl.getShortenedUrl().getClickCount(),
                userUrl.getCreatedAt(),
                userUrl.getUpdatedAt()
        );
    }

    private long getCacheVersion(UUID userId) {
        String versionKey = "user:cache_version:" + userId;
        String version = redisTemplate.opsForValue().get(versionKey);
        return version != null ? Long.parseLong(version) : 0L;
    }

    private void clearUserUrlsCache(UUID userId) {
        String versionKey = "user:cache_version:" + userId;
        redisTemplate.opsForValue().increment(versionKey);
        redisTemplate.expire(versionKey, Duration.ofDays(7));
    }

}
