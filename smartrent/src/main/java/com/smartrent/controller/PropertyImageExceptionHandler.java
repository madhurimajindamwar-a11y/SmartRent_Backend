package com.smartrent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(
        assignableTypes = PropertyImageController.class
)
public class PropertyImageExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleRejectedRequest(
            ResponseStatusException exception) {

        String message = exception.getReason();

        if (message == null || message.isBlank()) {
            message = "The photo request could not be completed.";
        }

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.<String, Object>of(
                        "status", exception.getStatusCode().value(),
                        "message", message
                ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingFile(
            MissingServletRequestPartException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.<String, Object>of(
                        "status", 400,
                        "message",
                        "The upload is missing the required file field: "
                                + exception.getRequestPartName()
                ));
    }
}