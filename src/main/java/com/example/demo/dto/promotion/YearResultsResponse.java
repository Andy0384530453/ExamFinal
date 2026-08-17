package com.example.demo.dto.promotion;

import java.util.UUID;

public record YearResultsResponse(
    Integer year, Double average, UUID bestStudentId, Double failureRate) {}
