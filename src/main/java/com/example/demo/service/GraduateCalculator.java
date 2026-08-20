package com.example.demo.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GraduateCalculator {

  private static final double PASSING_AVERAGE = 10.0;

  private final TranscriptDataBuilder transcriptDataBuilder;
  private final Rounder rounder;

  public GraduateCalculator(TranscriptDataBuilder transcriptDataBuilder, Rounder rounder) {
    this.transcriptDataBuilder = transcriptDataBuilder;
    this.rounder = rounder;
  }

  public Optional<Double> passingAverage(UUID studentId, UUID promotionId) {
    double average = computeAverage(studentId, promotionId);
    if (average < PASSING_AVERAGE) {
      return Optional.empty();
    }
    return Optional.of(rounder.round(average));
  }

  private double computeAverage(UUID studentId, UUID promotionId) {
    return transcriptDataBuilder.buildItems(studentId, promotionId).stream()
        .mapToDouble(item -> item.getGrade() == null ? 0.0 : item.getGrade())
        .average()
        .orElse(0.0);
  }
}
