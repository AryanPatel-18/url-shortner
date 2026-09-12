package com.aryan.url_shortner.service.url;

import com.aryan.url_shortner.exceptions.ShortenedUrlNotFoundException;
import com.aryan.url_shortner.model.ShortenedUrl;
import com.aryan.url_shortner.repository.ShortenedUrlRepository;
import com.aryan.url_shortner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortenedUrlService {

    private final ShortenedUrlRepository shortenedUrlRepository;
    private final UserRepository userRepository;


}