package com.example.demo.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final HttpServletRequest request = mock(HttpServletRequest.class);

  private void stubUri(String uri) {
    when(request.getRequestURI()).thenReturn(uri);
  }

  @Test
  void not_found_returns_404_with_message() {
    stubUri("/students/123/transcript");

    ResponseEntity<ErrorResponse> response =
        handler.handleNotFound(new ResourceNotFoundException("Student not found"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(404);
    assertThat(body.message()).isEqualTo("Student not found");
    assertThat(body.path()).isEqualTo("/students/123/transcript");
    assertThat(body.timestamp()).isNotNull();
  }

  @Test
  void access_denied_returns_403_with_message() {
    stubUri("/students/123/transcript");

    ResponseEntity<ErrorResponse> response =
        handler.handleAccessDenied(new AccessDeniedException("Forbidden"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(403);
    assertThat(body.message()).isEqualTo("Forbidden");
  }

  @Test
  void type_mismatch_returns_400_with_parameter_name() {
    stubUri("/students/not-a-uuid/transcript");
    MethodArgumentTypeMismatchException e = mock(MethodArgumentTypeMismatchException.class);
    when(e.getName()).thenReturn("id");

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(e, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.message()).contains("id");
  }

  @Test
  void unreadable_body_returns_400() {
    stubUri("/students/123/transcript");

    ResponseEntity<ErrorResponse> response =
        handler.handleNotReadable(new HttpMessageNotReadableException("bad json"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.message()).isEqualTo("Malformed request body");
  }

  @Test
  void validation_errors_return_400_with_field_messages() {
    stubUri("/students/123/transcript");
    MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
    bindingResult.addError(new FieldError("req", "promotionId", "must not be null"));
    when(e.getBindingResult()).thenReturn(bindingResult);

    ResponseEntity<ErrorResponse> response = handler.handleValidation(e, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(400);
    assertThat(body.message()).isEqualTo("promotionId: must not be null");
  }

  @Test
  void generic_exception_returns_500_without_leaking_internals() {
    stubUri("/students/123/transcript");

    ResponseEntity<ErrorResponse> response =
        handler.handleGeneric(new IllegalStateException("secret db details"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(500);
    assertThat(body.message()).isEqualTo("Internal Server Error");
    assertThat(body.message()).doesNotContain("secret db details");
  }
}
