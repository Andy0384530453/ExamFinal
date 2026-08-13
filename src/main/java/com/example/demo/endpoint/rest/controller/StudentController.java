package com.example.demo.endpoint.rest.controller;

import com.example.demo.endpoint.rest.model.Transcript;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StudentController {

  private static final String ROLE_STUDENT = "STUDENT";

  @GetMapping("/students/{id}/transcript")
  public Transcript getStudentTranscript(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") String id,
      @RequestParam(value = "promotionId", required = false) String promotionId) {
    if (ROLE_STUDENT.equals(jwt.getClaimAsString("role")) && !jwt.getSubject().equals(id)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "A student can only access their own transcript");
    }
    return new Transcript(
        UUID.randomUUID().toString(),
        id,
        promotionId,
        "PENDING",
        null,
        null,
        Instant.now(),
        List.of());
  }
}
