package com.example.demo.service;

import com.example.demo.dto.promotion.PromotionResultsResponse;
import com.example.demo.dto.promotion.YearResultsResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PromotionResultsServiceImpl implements PromotionResultsService {

  private static final double PASSING_AVERAGE = 10.0;

  private final JPromotionRepository promotionRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupRepository studentGroupRepository;
  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final JGradeRepository gradeRepository;
  private final GraduateCalculator graduateCalculator;

  public PromotionResultsServiceImpl(
      JPromotionRepository promotionRepository,
      JGroupRepository groupRepository,
      JStudentGroupRepository studentGroupRepository,
      JCourseRepository courseRepository,
      JExamRepository examRepository,
      JGradeRepository gradeRepository,
      GraduateCalculator graduateCalculator) {
    this.promotionRepository = promotionRepository;
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.graduateCalculator = graduateCalculator;
  }

  @Override
  public PromotionResultsResponse getPromotionResults(UUID promotionId) {
    JPromotion promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Promotion not found with id: " + promotionId));
    List<UUID> cohortStudentIds = findCohortStudentIds(promotionId);
    int baseYear = promotion.getYear();
    YearResultsResponse year1 = yearResults(baseYear, cohortStudentIds);
    YearResultsResponse year2 = yearResults(baseYear + 1, cohortStudentIds);
    YearResultsResponse year3 = yearResults(baseYear + 2, cohortStudentIds);
    Double overallAverage = averageOf(year1.average(), year2.average(), year3.average());
    double graduationRate = graduationRate(cohortStudentIds);
    return new PromotionResultsResponse(
        promotionId, baseYear, year1, year2, year3, overallAverage, graduationRate);
  }

  private List<UUID> findCohortStudentIds(UUID promotionId) {
    List<JGroup> groups = groupRepository.findByPromotionId(promotionId);
    if (groups.isEmpty()) {
      return List.of();
    }
    List<UUID> groupIds = groups.stream().map(JGroup::getId).toList();
    return studentGroupRepository.findByGroupIdIn(groupIds).stream()
        .map(JStudentGroup::getStudentId)
        .distinct()
        .toList();
  }

  private YearResultsResponse yearResults(int year, List<UUID> cohortStudentIds) {
    Optional<JPromotion> yearPromotion = promotionRepository.findByYear(year);
    if (yearPromotion.isEmpty()) {
      return new YearResultsResponse(year, null, null, null);
    }
    List<JCourse> courses = courseRepository.findByPromotionId(yearPromotion.get().getId());
    if (courses.isEmpty()) {
      return new YearResultsResponse(year, null, null, null);
    }
    List<UUID> courseIds = courses.stream().map(JCourse::getId).toList();
    List<JExam> exams = examRepository.findByCourseIdIn(courseIds);
    if (exams.isEmpty()) {
      return new YearResultsResponse(year, null, null, null);
    }
    List<UUID> examIds = exams.stream().map(JExam::getId).toList();
    List<JGrade> cohortGrades =
        gradeRepository.findByExamIdIn(examIds).stream()
            .filter(grade -> cohortStudentIds.contains(grade.getStudentId()))
            .toList();
    Map<UUID, Double> studentAverages = averagePerStudent(cohortGrades);
    if (studentAverages.isEmpty()) {
      return new YearResultsResponse(year, null, null, null);
    }
    double yearAverage =
        round(
            studentAverages.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
    UUID bestStudentId =
        studentAverages.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    long failing =
        studentAverages.values().stream().filter(average -> average < PASSING_AVERAGE).count();
    double failureRate = round(failing * 100.0 / studentAverages.size());
    return new YearResultsResponse(year, yearAverage, bestStudentId, failureRate);
  }

  private Map<UUID, Double> averagePerStudent(List<JGrade> grades) {
    Map<UUID, List<JGrade>> gradesByStudent =
        grades.stream().collect(Collectors.groupingBy(JGrade::getStudentId));
    Map<UUID, Double> averages = new HashMap<>();
    gradesByStudent.forEach(
        (studentId, studentGrades) ->
            averages.put(
                studentId,
                round(studentGrades.stream().mapToDouble(JGrade::getValue).average().orElse(0.0))));
    return averages;
  }

  private Double averageOf(Double... values) {
    List<Double> nonNull = Arrays.stream(values).filter(Objects::nonNull).toList();
    if (nonNull.isEmpty()) {
      return null;
    }
    return round(nonNull.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
  }

  private double graduationRate(List<UUID> cohortStudentIds) {
    if (cohortStudentIds.isEmpty()) {
      return 0.0;
    }
    long graduates =
        cohortStudentIds.stream()
            .filter(studentId -> graduateCalculator.passingAverage(studentId, null).isPresent())
            .count();
    return round(graduates * 100.0 / cohortStudentIds.size());
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
