package com.aryan.url_shortner.model;

import com.aryan.url_shortner.enums.UrlStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "shortened_urls",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_original_url", columnNames = "original_url"),
                @UniqueConstraint(name = "uk_short_code", columnNames = "short_code")
        }
)
@Getter
@Setter
public class ShortenedUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, length = 20)
    private String shortCode;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UrlStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        status = UrlStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
