package com.aryan.url_shortner.controller;
import com.aryan.url_shortner.dto.CreateShortUrlRequest;
import com.aryan.url_shortner.dto.ShortUrlResponse;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.service.security.CustomUserDetails;
import com.aryan.url_shortner.service.url.IShortenedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/${api.version}/urls")
@RequiredArgsConstructor
public class UrlController<IUrlService> {

    private final IShortenedUrlService urlsService;

    @PostMapping
    public ShortUrlResponse createShortUrl(@RequestBody CreateShortUrlRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        ShortenedUrl url =
                urlsService.getOrCreateShortenedUrl(request.originalUrl());

        return new ShortUrlResponse(
                user.getId(),
                url.getOriginalUrl(),
                url.getShortCode()
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        assert userDetails != null;
        return userDetails.getUser();
    }
}