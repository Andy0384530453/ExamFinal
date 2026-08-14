package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      ResourceNotFoundException e, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException e, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, e.getMessage(), request);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String message, HttpServletRequest request) {
    ErrorResponse body =
        new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
