package com.aryan.url_shortner.controller;
import com.aryan.url_shortner.dto.*;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.service.security.CustomUserDetails;
import com.aryan.url_shortner.service.url.IShortenedUrlService;
import com.aryan.url_shortner.service.userUrl.IUserUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/${api.version}/urls")
@RequiredArgsConstructor
public class UrlController<IUrlService> {

    private final IUserUrlService userUrlService;

    @PostMapping
    public ShortUrlResponse createShortUrl(
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

        return new ShortUrlResponse(
                userUrl.getId(),
                url.getOriginalUrl(),
                url.getShortCode()
        );
    }

    @GetMapping
    public UserUrlsResponse getUserUrls(
            Authentication authentication
    ) {

        User user = getAuthenticatedUser(authentication);

        return userUrlService.getUserUrls(user.getId());
    }

    @DeleteMapping
    public ResponseEntity<Void> removeUserUrl(@RequestBody DeleteUrlRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        userUrlService.removeUserUrl(
                user.getId(),
                request.urlId()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{urlId}")
    public UserUrlResponse getUserUrl(@PathVariable UUID urlId, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        return userUrlService.getUserUrl(
                user.getId(),
                urlId
        );
    }

    @PatchMapping("/{urlId}")
    public UserUrlResponse updateUrlStatus(
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

        return new UserUrlResponse(
                userUrl.getId(),
                url.getOriginalUrl(),
                url.getShortCode(),
                url.getStatus(),
                url.getClickCount(),
                userUrl.getCreatedAt(),
                userUrl.getUpdatedAt()
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        assert userDetails != null;
        return userDetails.getUser();
    }
}
