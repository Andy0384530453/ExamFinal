package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.JTranscriptItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraduateCalculatorTest {

  private TranscriptDataBuilder transcriptDataBuilder;
  private GraduateCalculator calculator;

  @BeforeEach
  void setUp() {
    transcriptDataBuilder = mock(TranscriptDataBuilder.class);
    calculator = new GraduateCalculator(transcriptDataBuilder, new Rounder());
  }

  @Test
  void returns_rounded_average_when_passing() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    when(transcriptDataBuilder.buildItems(studentId, promotionId))
        .thenReturn(List.of(item(12.0), item(14.0)));

    Optional<Double> average = calculator.passingAverage(studentId, promotionId);

    assertThat(average).contains(13.0);
  }

  @Test
  void rounds_average_to_two_decimals() {
    UUID studentId = UUID.randomUUID();
    when(transcriptDataBuilder.buildItems(studentId, null))
        .thenReturn(List.of(item(10.345), item(10.345)));

    Optional<Double> average = calculator.passingAverage(studentId, null);

    assertThat(average).contains(10.35);
  }

  @Test
  void returns_empty_when_average_below_ten() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    when(transcriptDataBuilder.buildItems(studentId, promotionId))
        .thenReturn(List.of(item(9.0), item(8.0)));

    Optional<Double> average = calculator.passingAverage(studentId, promotionId);

    assertThat(average).isEmpty();
  }

  @Test
  void returns_empty_when_no_items() {
    UUID studentId = UUID.randomUUID();
    when(transcriptDataBuilder.buildItems(studentId, null)).thenReturn(List.of());

    Optional<Double> average = calculator.passingAverage(studentId, null);

    assertThat(average).isEmpty();
  }

  private static JTranscriptItem item(double grade) {
    JTranscriptItem item = new JTranscriptItem();
    item.setId(UUID.randomUUID());
    item.setCourseTitle("Maths");
    item.setExamDate(Instant.parse("2024-01-01T09:00:00Z"));
    item.setCoefficient(1.0);
    item.setGrade(grade);
    item.setCredits(6);
    return item;
  }
}
