package com.aryan.url_shortner.service.userUrl;

import com.aryan.url_shortner.dto.UserUrlResponse;
import com.aryan.url_shortner.dto.UserUrlsResponse;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.UserUrl;

import java.util.List;
import java.util.UUID;

public interface IUserUrlService {

    UserUrl addUrlToUser(UUID userId, ShortenedUrl shortenedUrl);

    UserUrlsResponse getUserUrls(UUID userId);

    void removeUserUrl(UUID userId, UUID urlId);

    UserUrl createUserUrl(UUID userId, String originalUrl);

    UserUrlResponse getUserUrl(UUID userId, UUID urlId);

    UserUrl updateStatus(UUID userId, UUID urlId, UrlStatus status);
}