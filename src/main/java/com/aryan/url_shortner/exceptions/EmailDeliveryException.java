package com.aryan.url_shortner.exceptions;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
