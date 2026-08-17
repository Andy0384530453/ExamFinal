package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.dto.transcript.TranscriptSendEmailResponse;
import com.example.demo.service.TranscriptService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  @GetMapping("/transcripts/{id}")
  public TranscriptResponse getTranscript(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return transcriptService.getTranscript(id, jwt);
  }

  @GetMapping("/students/{id}/transcript/pdf")
  public ResponseEntity<byte[]> downloadTranscriptPdf(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    byte[] pdf = transcriptService.downloadTranscriptPdf(id, jwt);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transcript.pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  @PostMapping("/students/{id}/transcript/send-email")
  public ResponseEntity<TranscriptSendEmailResponse> sendStudentTranscriptEmail(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(transcriptService.requestTranscriptEmail(id, jwt));
  }
}
