package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.model.ShortenedUrl;

import java.util.UUID;

public interface IShortenedUrlService {
    public ShortenedUrl getById(UUID id);
    public ShortenedUrl getByShortCode(String shortCode);
    public boolean shortCodeExists(String shortCode);
}
