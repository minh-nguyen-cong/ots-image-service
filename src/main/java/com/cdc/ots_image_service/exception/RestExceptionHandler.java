package com.cdc.ots_image_service.exception;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class RestExceptionHandler {

    private final MessageSource messageSource;

    public RestExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(ImageNotFoundException ex, WebRequest request) {
        String message = messageSource.getMessage(ex.getMessage(), new Object[]{ex.getId()}, request.getLocale());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), message);
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleImageAccessDenied(ImageAccessDeniedException ex, WebRequest request) {
        String message = messageSource.getMessage(ex.getMessage(), null, request.getLocale());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), message);
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // A simple DTO for a consistent error response format
    private static class ErrorResponse {
        private final int status;
        private final String message;

        public ErrorResponse(int status, String message) { this.status = status; this.message = message; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
    }
}