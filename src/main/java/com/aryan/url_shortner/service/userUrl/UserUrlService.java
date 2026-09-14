package com.aryan.url_shortner.service.userUrl;

import com.aryan.url_shortner.dto.UserUrlResponse;
import com.aryan.url_shortner.dto.UserUrlsResponse;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.exceptions.UserUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.repository.UserUrlRepository;
import com.aryan.url_shortner.service.url.IShortenedUrlService;
import com.aryan.url_shortner.service.user.IUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserUrlService implements IUserUrlService{

    private final UserUrlRepository userUrlRepository;
    private final IUserService userService;
    private final IShortenedUrlService shortenedUrlService;

    @Override
    public UserUrl addUrlToUser(UUID userId, ShortenedUrl shortenedUrl) {

        Optional<UserUrl> existing = userUrlRepository
                .findByUserIdAndShortenedUrlId(
                        userId,
                        shortenedUrl.getId()
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userService.getUser(userId);

        UserUrl userUrl = new UserUrl();
        userUrl.setUser(user);
        userUrl.setShortenedUrl(shortenedUrl);

        return userUrlRepository.save(userUrl);
    }

    @Override
    public UserUrlsResponse getUserUrls(UUID userId) {

        List<UserUrlResponse> urls = userUrlRepository.findByUserId(userId)
                .stream()
                .map(userUrl -> new UserUrlResponse(
                        userUrl.getId(),
                        userUrl.getShortenedUrl().getOriginalUrl(),
                        userUrl.getShortenedUrl().getShortCode(),
                        userUrl.getShortenedUrl().getStatus(),
                        userUrl.getShortenedUrl().getClickCount(),
                        userUrl.getCreatedAt(),
                        userUrl.getUpdatedAt()
                ))
                .toList();

        return new UserUrlsResponse(urls);
    }

    @Transactional
    @Override
    public UserUrl updateStatus(UUID userId, UUID urlId, UrlStatus status) {
        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("User URL not found"));

        userUrl.getShortenedUrl().setStatus(status);

        return userUrlRepository.save(userUrl);
    }

    @Override
    public void removeUserUrl(UUID userId, UUID urlId) {

        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("URL not found for user"));

        userUrlRepository.delete(userUrl);
    }

    @Override
    public UserUrl createUserUrl(UUID userId, String originalUrl) {
        ShortenedUrl shortenedUrl =
                shortenedUrlService.getOrCreateShortenedUrl(originalUrl);

        return addUrlToUser(userId, shortenedUrl);
    }

    @Override
    public UserUrlResponse getUserUrl(UUID userId, UUID urlId) {

        UserUrl userUrl = userUrlRepository
                .findByUserIdAndId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("URL not found for user"));

        return new UserUrlResponse(
                userUrl.getId(),
                userUrl.getShortenedUrl().getOriginalUrl(),
                userUrl.getShortenedUrl().getShortCode(),
                userUrl.getShortenedUrl().getStatus(),
                userUrl.getShortenedUrl().getClickCount(),
                userUrl.getCreatedAt(),
                userUrl.getUpdatedAt()
        );
    }


}
