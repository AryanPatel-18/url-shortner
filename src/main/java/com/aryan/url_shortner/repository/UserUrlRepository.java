package com.aryan.url_shortner.repository;

import com.aryan.url_shortner.model.UserUrl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserUrlRepository extends JpaRepository<UserUrl, Long> {

}
