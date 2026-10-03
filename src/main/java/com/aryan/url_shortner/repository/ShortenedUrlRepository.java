package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.dto.UrlStats;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("SELECT COALESCE(SUM(s.clickCount), 0) FROM ShortenedUrl s")
    long sumTotalClicks();
    @Query("""
        SELECT new com.aryan.url_shortner.dto.UrlStats(
            COUNT(s),
            COALESCE(SUM(CASE WHEN s.status = :activeStatus THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.status = :disabledStatus THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.createdAt >= :today THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.createdAt >= :sevenDays THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN s.createdAt >= :thirtyDays THEN 1L ELSE 0L END), 0L)
        )
        FROM ShortenedUrl s
    """)
    UrlStats getUrlStatistics(
            @Param("activeStatus") UrlStatus activeStatus,
            @Param("disabledStatus") UrlStatus disabledStatus,
            @Param("today") LocalDateTime today,
            @Param("sevenDays") LocalDateTime sevenDays,
            @Param("thirtyDays") LocalDateTime thirtyDays
    );
}