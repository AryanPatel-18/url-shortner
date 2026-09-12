package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, UUID> {

    Optional<ShortenedUrl> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    Optional<ShortenedUrl> findByOriginalUrl(String originalUrl);

    @Modifying
    @Query("""
    UPDATE ShortenedUrl s
    SET s.clickCount = s.clickCount + 1
    WHERE s.id = :id
""")
    void incrementClickCount(@Param("id") UUID id);
}