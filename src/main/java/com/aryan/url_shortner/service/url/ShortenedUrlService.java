package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.dto.RedirectCacheDTO;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.exceptions.ShortenedUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ShortenedUrlService implements IShortenedUrlService{

    private static final int SHORT_CODE_LENGTH = 6;
    private final ShortenedUrlRepository shortenedUrlRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String ACTIVE_KEY = "url:clicks:active";

    @Override
    public ShortenedUrl getOrCreateShortenedUrl(String originalUrl) {
        Optional<ShortenedUrl> existingUrl = shortenedUrlRepository.findByOriginalUrl(originalUrl);

        if (existingUrl.isPresent()) {
            return existingUrl.get();
        }

        ShortenedUrl shortenedUrl = shortenUrl(originalUrl);

        return shortenedUrlRepository.save(shortenedUrl);
    }

    @Override
    public ShortenedUrl shortenUrl(String originalUrl) {
        while (true) {
            ShortenedUrl shortenedUrl = new ShortenedUrl();

            shortenedUrl.setOriginalUrl(originalUrl);
            shortenedUrl.setShortCode(generateShortCode());

            try {
                return shortenedUrlRepository.saveAndFlush(shortenedUrl);

            } catch (DataIntegrityViolationException e) {

                if (isShortCodeCollision(e)) {
                    continue;
                }

                throw e;
            }
        }
    }

    @Override
    public RedirectCacheDTO getByShortCode(String shortCode) {

        String key = "redirect:" + shortCode;
        String cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue != null) {
            try {
                String[] parts = cachedValue.split(":::", 3);

                if (parts.length == 3) {
                    return new RedirectCacheDTO(
                            UUID.fromString(parts[0]),
                            parts[2],
                            UrlStatus.valueOf(parts[1])
                    );
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid cache entry. Fall back to database.
            }
        }

        ShortenedUrl shortenedUrl =
                shortenedUrlRepository.findByShortCode(shortCode)
                        .orElseThrow(() ->
                                new ShortenedUrlNotFoundException(
                                        "Short URL not found"
                                ));

        RedirectCacheDTO redirectCacheDTO =
                new RedirectCacheDTO(
                        shortenedUrl.getId(),
                        shortenedUrl.getOriginalUrl(),
                        shortenedUrl.getStatus()
                );

        String cacheData = shortenedUrl.getId() + ":::" +
                           shortenedUrl.getStatus().name() + ":::" +
                           shortenedUrl.getOriginalUrl();

        redisTemplate.opsForValue().set(
                key,
                cacheData,
                Duration.ofHours(1)
        );

        return redirectCacheDTO;
    }


    @Override
    public void incrementClickCount(UUID urlId) {
        try {
            redisTemplate.opsForHash().increment(
                    ACTIVE_KEY,
                    urlId.toString(),
                    1
            );
        } catch (Exception e) {
            shortenedUrlRepository.incrementClickCount(urlId);
        }
    }

    private String generateShortCode() {

        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder shortCode = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(characters.length());
            shortCode.append(characters.charAt(index));
        }

        return shortCode.toString();
    }

    private boolean isShortCodeCollision(DataIntegrityViolationException exception) {

        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof org.postgresql.util.PSQLException postgresException) {

                assert postgresException.getServerErrorMessage() != null;
                return "uk_short_code".equals(
                        postgresException.getServerErrorMessage()
                                .getConstraint()
                );
            }

            cause = cause.getCause();
        }

        return false;
    }
}