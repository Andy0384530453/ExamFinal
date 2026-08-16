package com.example.demo.dto.grade;

import java.time.Instant;
import java.util.UUID;

public record GradeHistoryResponse(
    Double oldValue, Double newValue, String reason, Instant modifiedAt, UUID modifiedBy) {}
