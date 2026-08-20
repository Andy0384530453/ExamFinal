package com.example.demo.service;

import com.example.demo.dto.promotion.PromotionResultsResponse;
import com.example.demo.dto.promotion.YearResultsResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.validator.EntityValidator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromotionResultsServiceImpl implements PromotionResultsService {

  private final JPromotionRepository promotionRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupRepository studentGroupRepository;
  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final JGradeRepository gradeRepository;
  private final PromotionResultsCalculator calculator;
  private final EntityValidator validator;

  public PromotionResultsServiceImpl(
      JPromotionRepository promotionRepository,
      JGroupRepository groupRepository,
      JStudentGroupRepository studentGroupRepository,
      JCourseRepository courseRepository,
      JExamRepository examRepository,
      JGradeRepository gradeRepository,
      PromotionResultsCalculator calculator,
      EntityValidator validator) {
    this.promotionRepository = promotionRepository;
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
    this.courseRepository = courseRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.calculator = calculator;
    this.validator = validator;
  }

  @Override
  public PromotionResultsResponse getPromotionResults(UUID promotionId) {
    JPromotion promotion = validator.requirePromotion(promotionId);
    List<UUID> cohortStudentIds = findCohortStudentIds(promotionId);
    int baseYear = promotion.getYear();
    YearResultsResponse year1 = yearResults(baseYear, cohortStudentIds);
    YearResultsResponse year2 = yearResults(baseYear + 1, cohortStudentIds);
    YearResultsResponse year3 = yearResults(baseYear + 2, cohortStudentIds);
    Double overallAverage =
        calculator.overallAverage(year1.average(), year2.average(), year3.average());
    double graduationRate = calculator.graduationRate(cohortStudentIds);
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
    Map<UUID, Double> studentAverages = calculator.averagePerStudent(cohortGrades);
    if (studentAverages.isEmpty()) {
      return new YearResultsResponse(year, null, null, null);
    }
    double yearAverage = calculator.yearlyAverage(studentAverages);
    UUID bestStudentId = calculator.bestStudent(studentAverages).orElse(null);
    double failureRate = calculator.failureRate(studentAverages);
    return new YearResultsResponse(year, yearAverage, bestStudentId, failureRate);
  }
}
