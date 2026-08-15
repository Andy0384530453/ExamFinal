package com.example.demo.endpoint.rest.model;

import java.time.Instant;
import java.util.List;

public record Transcript(
    String id,
    String studentId,
    String promotionId,
    String status,
    String pdfUrl,
    String email,
    Instant generatedAt,
    List<TranscriptItem> items) {}
