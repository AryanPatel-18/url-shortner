package com.aryan.url_shortner.exceptions;

public class UserUrlNotFoundException extends RuntimeException {
    public UserUrlNotFoundException(String message) {
        super(message);
    }
}
