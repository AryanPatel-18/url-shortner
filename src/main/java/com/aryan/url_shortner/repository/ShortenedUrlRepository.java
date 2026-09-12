package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, UUID> {

    Optional<ShortenedUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);
}