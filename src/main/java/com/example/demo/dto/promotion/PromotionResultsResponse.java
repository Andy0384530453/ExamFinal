package com.example.demo.dto.promotion;

import java.util.UUID;

public record PromotionResultsResponse(
    UUID promotionId,
    Integer promotionYear,
    YearResultsResponse year1,
    YearResultsResponse year2,
    YearResultsResponse year3,
    Double overallAverage,
    Double graduationRate) {}
