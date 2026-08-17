package com.example.demo.dto.grade;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GradeCreateRequest(
    @NotNull UUID studentId,
    @NotNull UUID examId,
    @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double value,
    String comment) {}
