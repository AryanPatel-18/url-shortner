package com.aryan.url_shortner.service.userUrl;

import com.aryan.url_shortner.exceptions.UserUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import com.aryan.url_shortner.repository.UserUrlRepository;
import com.aryan.url_shortner.service.user.IUserService;
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
        userUrl.setStatus("ACTIVE");

        return userUrlRepository.save(userUrl);
    }

    @Override
    public List<UserUrl> getUserUrls(UUID userId) {
        return userUrlRepository.findByUserId(userId);
    }

    @Override
    public UserUrl updateStatus(UUID userId, UUID urlId, String status) {
        UserUrl userUrl = userUrlRepository
                .findByUserIdAndShortenedUrlId(userId, urlId)
                .orElseThrow(() ->
                        new UserUrlNotFoundException("User URL not found"));

        userUrl.setStatus(status);

        return userUrlRepository.save(userUrl);
    }

    @Override
    public void removeUserUrl(UUID userId, UUID urlId) {

        UserUrl userUrl = userUrlRepository
                .findByUserIdAndShortenedUrlId(userId, urlId)
                .orElseThrow(() -> new UserUrlNotFoundException("URL not found for user"));

        userUrlRepository.delete(userUrl);
    }
}
