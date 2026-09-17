package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.dto.RedirectCacheDTO;
import com.aryan.url_shortner.model.ShortenedUrl;

import java.util.UUID;

public interface IShortenedUrlService {
    ShortenedUrl getOrCreateShortenedUrl(String originalUrl);
    ShortenedUrl shortenUrl(String originalUrl);
    RedirectCacheDTO getByShortCode(String shortCode);
    void incrementClickCount(UUID urlId);
}
