package com.aryan.url_shortner.service.userUrl;

import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.UserUrl;

import java.util.List;
import java.util.UUID;

public interface IUserUrlService {

    UserUrl addUrlToUser(UUID userId, ShortenedUrl shortenedUrl);

    List<UserUrl> getUserUrls(UUID userId);

    UserUrl updateStatus(UUID userId, UUID urlId, String status);

    void removeUserUrl(UUID userId, UUID urlId);
}