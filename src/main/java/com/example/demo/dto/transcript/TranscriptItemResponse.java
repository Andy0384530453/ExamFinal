package com.example.demo.dto.transcript;

import java.time.Instant;

public record TranscriptItemResponse(
    String courseTitle, Instant examDate, double coefficient, Double grade, Integer credits) {}
