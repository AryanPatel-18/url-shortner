package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.UserUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserUrlRepository extends JpaRepository<UserUrl, Long> {
    Optional<UserUrl> findByUserIdAndShortenedUrlId(UUID userId, UUID urlId);
    List<UserUrl> findByUserId(UUID userId);
    boolean existsByUserIdAndShortenedUrlId(UUID userId, UUID urlId);
    Optional<UserUrl> findByUserIdAndId(UUID userId, UUID id);
}
