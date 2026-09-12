package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.exceptions.ShortenedUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import com.aryan.url_shortner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortenedUrlService {

    private final ShortenedUrlRepository shortenedUrlRepository;
    private final UserRepository userRepository;

    public ShortenedUrl getById(UUID id) {
        return shortenedUrlRepository.findById(id)
                .orElseThrow(() -> new ShortenedUrlNotFoundException("Not found id with value : " + id));
    }

    public ShortenedUrl getByShortCode(String shortCode) {
        return shortenedUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortenedUrlNotFoundException("Shortened URL not found with code " + shortCode));
    }

    public boolean shortCodeExists(String shortCode) {
        return shortenedUrlRepository.existsByShortCode(shortCode);
    }
}