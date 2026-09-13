package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.exceptions.ShortenedUrlNotFoundException;
import com.aryan.url_shortner.exceptions.UserUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import com.aryan.url_shortner.repository.UserUrlRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ShortenedUrlService implements IShortenedUrlService{

    private static final int SHORT_CODE_LENGTH = 6;
    private final ShortenedUrlRepository shortenedUrlRepository;

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
    public ShortenedUrl getByShortCode(String shortCode) {
        return shortenedUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortenedUrlNotFoundException("Short URL not found"));
    }

    @Transactional
    @Override
    public void incrementClickCount(ShortenedUrl shortenedUrl) {
        shortenedUrlRepository.incrementClickCount(shortenedUrl.getId());
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