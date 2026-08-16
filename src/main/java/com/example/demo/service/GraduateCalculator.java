package com.example.demo.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GraduateCalculator {

  private static final double PASSING_AVERAGE = 10.0;

  private final TranscriptDataBuilder transcriptDataBuilder;

  public GraduateCalculator(TranscriptDataBuilder transcriptDataBuilder) {
    this.transcriptDataBuilder = transcriptDataBuilder;
  }

  public Optional<Double> passingAverage(UUID studentId, UUID promotionId) {
    double average = computeAverage(studentId, promotionId);
    if (average < PASSING_AVERAGE) {
      return Optional.empty();
    }
    return Optional.of(round(average));
  }

  private double computeAverage(UUID studentId, UUID promotionId) {
    return transcriptDataBuilder.buildItems(studentId, promotionId).stream()
        .mapToDouble(item -> item.getGrade() == null ? 0.0 : item.getGrade())
        .average()
        .orElse(0.0);
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
