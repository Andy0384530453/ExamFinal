package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.service.TranscriptService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranscriptController {

  private final TranscriptService transcriptService;

  public TranscriptController(TranscriptService transcriptService) {
    this.transcriptService = transcriptService;
  }

  @GetMapping("/students/{id}/transcript")
  public TranscriptResponse getStudentTranscript(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @RequestParam(value = "promotionId", required = false) UUID promotionId) {
    return transcriptService.getStudentTranscript(id, promotionId, jwt);
  }
}
