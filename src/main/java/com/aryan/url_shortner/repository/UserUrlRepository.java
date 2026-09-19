package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.UserUrl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserUrlRepository extends JpaRepository<UserUrl, Long> {
    Optional<UserUrl> findByUserIdAndShortenedUrlId(UUID userId, UUID urlId);

    @Query("""
        SELECT uu
        FROM UserUrl uu
        JOIN FETCH uu.shortenedUrl
        WHERE uu.user.id = :userId AND uu.id = :urlId
        """)
    List<UserUrl> findByUserIdWithShortenedUrl(@Param("userId") UUID userId, Pageable pageable);

    boolean existsByUserIdAndShortenedUrlId(UUID userId, UUID urlId);
    Optional<UserUrl> findByUserIdAndId(UUID userId, UUID id);
}
