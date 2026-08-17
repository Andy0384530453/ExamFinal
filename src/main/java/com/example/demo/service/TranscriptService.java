package com.example.demo.service;

import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.dto.transcript.TranscriptSendEmailResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface TranscriptService {

  TranscriptResponse getStudentTranscript(UUID studentId, UUID promotionId, Jwt jwt);

  TranscriptResponse getTranscript(UUID transcriptId, Jwt jwt);

  byte[] downloadTranscriptPdf(UUID studentId, Jwt jwt);

  TranscriptSendEmailResponse requestTranscriptEmail(UUID studentId, Jwt jwt);
}
