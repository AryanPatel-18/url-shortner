package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.User;
import com.aryan.url_shortner.model.UserUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}