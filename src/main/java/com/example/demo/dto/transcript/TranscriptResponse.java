package com.example.demo.dto.transcript;

import com.example.demo.enums.TranscriptStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
    UUID id,
    UUID studentId,
    UUID promotionId,
    TranscriptStatus status,
    String pdfUrl,
    String email,
    Instant generatedAt,
    List<TranscriptItemResponse> items) {}
