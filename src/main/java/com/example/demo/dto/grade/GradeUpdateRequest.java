package com.example.demo.dto.grade;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradeUpdateRequest(
    @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Double value, @NotBlank String reason) {}
