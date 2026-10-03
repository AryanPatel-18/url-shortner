package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.dto.UserStats;
import com.aryan.url_shortner.enums.Role;
import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @Query("""
        SELECT new com.aryan.url_shortner.dto.UserStats(
            COUNT(u),
            COALESCE(SUM(CASE WHEN u.role = :adminRole THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN u.role = :userRole THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN u.createdAt >= :today THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN u.createdAt >= :sevenDays THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN u.createdAt >= :thirtyDays THEN 1L ELSE 0L END), 0L)
        )
        FROM User u
    """)
    UserStats getUserStatistics(
            @Param("adminRole") Role adminRole,
            @Param("userRole") Role userRole,
            @Param("today") Instant today,
            @Param("sevenDays") Instant sevenDays,
            @Param("thirtyDays") Instant thirtyDays
    );
}