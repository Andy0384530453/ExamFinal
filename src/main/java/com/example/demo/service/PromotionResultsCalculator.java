package com.example.demo.service;

import com.example.demo.entity.JGrade;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PromotionResultsCalculator {

  public static final double PASSING_AVERAGE = 10.0;

  private final GraduateCalculator graduateCalculator;
  private final Rounder rounder;

  public PromotionResultsCalculator(GraduateCalculator graduateCalculator, Rounder rounder) {
    this.graduateCalculator = graduateCalculator;
    this.rounder = rounder;
  }

  public Map<UUID, Double> averagePerStudent(List<JGrade> grades) {
    Map<UUID, List<JGrade>> gradesByStudent =
        grades.stream().collect(Collectors.groupingBy(JGrade::getStudentId));
    Map<UUID, Double> averages = new HashMap<>();
    gradesByStudent.forEach(
        (studentId, studentGrades) ->
            averages.put(
                studentId,
                rounder.round(
                    studentGrades.stream().mapToDouble(JGrade::getValue).average().orElse(0.0))));
    return averages;
  }

  public double yearlyAverage(Map<UUID, Double> studentAverages) {
    return rounder.round(
        studentAverages.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
  }

  public Optional<UUID> bestStudent(Map<UUID, Double> studentAverages) {
    return studentAverages.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  public double failureRate(Map<UUID, Double> studentAverages) {
    long failing =
        studentAverages.values().stream().filter(average -> average < PASSING_AVERAGE).count();
    return rounder.roundToPercentage(failing, studentAverages.size());
  }

  public Double overallAverage(Double... values) {
    List<Double> nonNull = Arrays.stream(values).filter(Objects::nonNull).toList();
    if (nonNull.isEmpty()) {
      return null;
    }
    return rounder.round(nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
  }

  public double graduationRate(List<UUID> cohortStudentIds) {
    if (cohortStudentIds.isEmpty()) {
      return 0.0;
    }
    long graduates =
        cohortStudentIds.stream()
            .filter(studentId -> graduateCalculator.passingAverage(studentId, null).isPresent())
            .count();
    return rounder.roundToPercentage(graduates, cohortStudentIds.size());
  }
}
