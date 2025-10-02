package com.cdc.ots_image_service.exception;

public class ImageAccessDeniedException extends RuntimeException {
    public ImageAccessDeniedException(String message) {
        super(message);
    }
}