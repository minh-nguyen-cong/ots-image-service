package com.cdc.ots_image_service.exception;

public class ImageNotFoundException extends RuntimeException {
    private final Long id;

    public ImageNotFoundException(String message, Long id) {
        super(message);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}