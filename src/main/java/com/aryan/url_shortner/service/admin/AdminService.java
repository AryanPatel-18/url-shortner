package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.AdminBusinessStatisticsResponse;
import com.aryan.url_shortner.dto.AdminTestResponse;
import com.aryan.url_shortner.dto.UrlStats;
import com.aryan.url_shortner.dto.UserStats;
import com.aryan.url_shortner.enums.Role;
import com.aryan.url_shortner.enums.UrlStatus;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import com.aryan.url_shortner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminService implements IAdminService {

    private final UserRepository userRepository;
    private final ShortenedUrlRepository shortenedUrlRepository;

    @Override
    public AdminTestResponse getTestStatus() {
        return new AdminTestResponse(
                "Admin API Foundation is active and secure.",
                LocalDateTime.now()
        );
    }

    @Override
    public AdminBusinessStatisticsResponse getBusinessStatistics() {
        Instant now = Instant.now();
        UserStats userStats = userRepository.getUserStatistics(
                Role.ADMIN,
                Role.USER,
                now.minus(1, ChronoUnit.DAYS),
                now.minus(7, ChronoUnit.DAYS),
                now.minus(30, ChronoUnit.DAYS)
        );

        LocalDateTime localNow = LocalDateTime.now();
        UrlStats urlStats = shortenedUrlRepository.getUrlStatistics(
                UrlStatus.ACTIVE,
                UrlStatus.DISABLED,
                localNow.minusDays(1),
                localNow.minusDays(7),
                localNow.minusDays(30)
        );

        long totalClicks = shortenedUrlRepository.sumTotalClicks();

        return new AdminBusinessStatisticsResponse(
                new AdminBusinessStatisticsResponse.UserStatistics(
                        userStats.totalUsers(), userStats.adminUsers(), userStats.standardUsers(),
                        userStats.usersToday(), userStats.users7Days(), userStats.users30Days()
                ),
                new AdminBusinessStatisticsResponse.UrlStatistics(
                        urlStats.totalUrls(), urlStats.activeUrls(), urlStats.disabledUrls(),
                        urlStats.urlsToday(), urlStats.urls7Days(), urlStats.urls30Days()
                ),
                new AdminBusinessStatisticsResponse.ClickStatistics(
                        totalClicks,
                        "Historical daily click data cannot be accurately calculated from the current schema."
                )
        );
    }
}