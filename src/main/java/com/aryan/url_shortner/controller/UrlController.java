package com.aryan.url_shortner.controller;
import com.aryan.url_shortner.dto.*;
import com.aryan.url_shortner.enums.RateLimitOperation;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.model.CustomUserDetails;
import com.aryan.url_shortner.service.userUrl.IUserUrlService;
import com.aryan.url_shortner.annotation.RateLimit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/api/${api.version}/urls")
@RequiredArgsConstructor
public class UrlController<IUrlService> {

    private final IUserUrlService userUrlService;

    @PostMapping
    @RateLimit(operation = RateLimitOperation.CREATE)
    public ResponseEntity<ShortUrlResponse> createShortUrl(
            @RequestBody CreateShortUrlRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        UserUrl userUrl =
                userUrlService.createUserUrl(
                        user.getId(),
                        request.originalUrl()
                );

        ShortenedUrl url = userUrl.getShortenedUrl();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ShortUrlResponse(
                        userUrl.getId(),
                        url.getOriginalUrl(),
                        url.getShortCode()
                )
        );
    }

    @GetMapping
    @RateLimit(operation = RateLimitOperation.LIST)
    public ResponseEntity<UserUrlsResponse> getUserUrls(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 20) {
            return ResponseEntity.badRequest().build();
        }

        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                userUrlService.getUserUrls(user.getId(), page, size)
        );
    }

    @DeleteMapping
    @RateLimit(operation = RateLimitOperation.DELETE)
    public ResponseEntity<Void> removeUserUrl(
            @RequestBody DeleteUrlRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        userUrlService.removeUserUrl(
                user.getId(),
                request.urlId()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{urlId}")
    @RateLimit(operation = RateLimitOperation.GET)
    public ResponseEntity<UserUrlResponse> getUserUrl(
            @PathVariable UUID urlId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                userUrlService.getUserUrl(
                        user.getId(),
                        urlId
                )
        );
    }

    @PatchMapping("/{urlId}")
    @RateLimit(operation = RateLimitOperation.UPDATE)
    public ResponseEntity<UserUrlResponse> updateUrlStatus(
            @PathVariable UUID urlId,
            @RequestBody UpdateUrlStatusRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        UserUrl userUrl = userUrlService.updateStatus(
                user.getId(),
                urlId,
                request.status()
        );

        ShortenedUrl url = userUrl.getShortenedUrl();

        return ResponseEntity.ok(
                new UserUrlResponse(
                        userUrl.getId(),
                        url.getOriginalUrl(),
                        url.getShortCode(),
                        url.getStatus(),
                        url.getClickCount(),
                        userUrl.getCreatedAt(),
                        userUrl.getUpdatedAt()
                )
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        assert userDetails != null;
        return userDetails.getUser();
    }
}
