package com.example.demo.dto.grade;

import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
    UUID id,
    UUID studentId,
    String courseTitle,
    Double value,
    String comment,
    Instant modifiedAt,
    UUID modifiedBy) {}
