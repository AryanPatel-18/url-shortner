package com.aryan.url_shortner.controller;


import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.service.url.IShortenedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final IShortenedUrlService urlsService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode) {

        ShortenedUrl shortenedUrl =
                urlsService.getByShortCode(shortCode);

        if (shortenedUrl.getStatus() != UrlStatus.ACTIVE) {
            return ResponseEntity
                    .status(HttpStatus.GONE)
                    .build();
        }

        urlsService.incrementClickCount(shortenedUrl);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(shortenedUrl.getOriginalUrl()))
                .build();
    }
}