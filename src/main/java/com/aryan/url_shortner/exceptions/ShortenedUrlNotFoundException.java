package com.aryan.url_shortner.exceptions;

import java.util.UUID;

public class ShortenedUrlNotFoundException extends RuntimeException {
    public ShortenedUrlNotFoundException(String message) {
        super(message);
    }
}
