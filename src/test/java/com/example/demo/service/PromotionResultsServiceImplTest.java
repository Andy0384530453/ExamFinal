package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.promotion.PromotionResultsResponse;
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
import com.example.demo.validator.EntityValidator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromotionResultsServiceImplTest {

  private JPromotionRepository promotionRepository;
  private JGroupRepository groupRepository;
  private JStudentGroupRepository studentGroupRepository;
  private JCourseRepository courseRepository;
  private JExamRepository examRepository;
  private JGradeRepository gradeRepository;
  private GraduateCalculator graduateCalculator;
  private PromotionResultsServiceImpl service;

  @BeforeEach
  void setUp() {
    promotionRepository = mock(JPromotionRepository.class);
    groupRepository = mock(JGroupRepository.class);
    studentGroupRepository = mock(JStudentGroupRepository.class);
    courseRepository = mock(JCourseRepository.class);
    examRepository = mock(JExamRepository.class);
    gradeRepository = mock(JGradeRepository.class);
    graduateCalculator = mock(GraduateCalculator.class);
    EntityValidator validator =
        new EntityValidator(null, groupRepository, null, promotionRepository, null, null, null);
    service =
        new PromotionResultsServiceImpl(
            promotionRepository,
            groupRepository,
            studentGroupRepository,
            courseRepository,
            examRepository,
            gradeRepository,
            new PromotionResultsCalculator(graduateCalculator, new Rounder()),
            validator);
  }

  @Test
  void computes_results_over_the_three_years_of_the_cohort() {
    UUID promotionId = UUID.randomUUID();
    UUID s1 = UUID.randomUUID();
    UUID s2 = UUID.randomUUID();
    JPromotion promotion = promotion(promotionId, 2023);
    JPromotion promotion2024 = promotion(UUID.randomUUID(), 2024);
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(promotionRepository.findByYear(2023)).thenReturn(Optional.of(promotion));
    when(promotionRepository.findByYear(2024)).thenReturn(Optional.of(promotion2024));
    when(promotionRepository.findByYear(2025)).thenReturn(Optional.empty());

    JGroup group = group(promotionId);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(s1, group.getId()), membership(s2, group.getId())));

    JCourse course2023 = course(promotion.getId());
    JCourse course2024 = course(promotion2024.getId());
    when(courseRepository.findByPromotionId(promotion.getId())).thenReturn(List.of(course2023));
    when(courseRepository.findByPromotionId(promotion2024.getId())).thenReturn(List.of(course2024));
    JExam exam2023 = exam(course2023.getId());
    JExam exam2024 = exam(course2024.getId());
    when(examRepository.findByCourseIdIn(List.of(course2023.getId())))
        .thenReturn(List.of(exam2023));
    when(examRepository.findByCourseIdIn(List.of(course2024.getId())))
        .thenReturn(List.of(exam2024));
    when(gradeRepository.findByExamIdIn(List.of(exam2023.getId())))
        .thenReturn(List.of(grade(s1, exam2023.getId(), 14.0), grade(s2, exam2023.getId(), 8.0)));
    when(gradeRepository.findByExamIdIn(List.of(exam2024.getId())))
        .thenReturn(List.of(grade(s1, exam2024.getId(), 12.0), grade(s2, exam2024.getId(), 16.0)));

    when(graduateCalculator.passingAverage(s1, null)).thenReturn(Optional.of(13.0));
    when(graduateCalculator.passingAverage(s2, null)).thenReturn(Optional.empty());

    PromotionResultsResponse response = service.getPromotionResults(promotionId);

    assertThat(response.promotionId()).isEqualTo(promotionId);
    assertThat(response.promotionYear()).isEqualTo(2023);
    assertThat(response.year1().year()).isEqualTo(2023);
    assertThat(response.year1().average()).isEqualTo(11.0);
    assertThat(response.year1().bestStudentId()).isEqualTo(s1);
    assertThat(response.year1().failureRate()).isEqualTo(50.0);
    assertThat(response.year2().year()).isEqualTo(2024);
    assertThat(response.year2().average()).isEqualTo(14.0);
    assertThat(response.year2().bestStudentId()).isEqualTo(s2);
    assertThat(response.year2().failureRate()).isEqualTo(0.0);
    assertThat(response.year3().year()).isEqualTo(2025);
    assertThat(response.year3().average()).isNull();
    assertThat(response.overallAverage()).isEqualTo(12.5);
    assertThat(response.graduationRate()).isEqualTo(50.0);
  }

  @Test
  void year_without_any_cohort_grade_has_null_results() {
    UUID promotionId = UUID.randomUUID();
    UUID s1 = UUID.randomUUID();
    JPromotion promotion = promotion(promotionId, 2023);
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(promotionRepository.findByYear(2023)).thenReturn(Optional.of(promotion));
    when(promotionRepository.findByYear(2024)).thenReturn(Optional.empty());
    when(promotionRepository.findByYear(2025)).thenReturn(Optional.empty());

    JGroup group = group(promotionId);
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of(group));
    when(studentGroupRepository.findByGroupIdIn(List.of(group.getId())))
        .thenReturn(List.of(membership(s1, group.getId())));

    JCourse course2023 = course(promotion.getId());
    when(courseRepository.findByPromotionId(promotion.getId())).thenReturn(List.of(course2023));
    JExam exam2023 = exam(course2023.getId());
    when(examRepository.findByCourseIdIn(List.of(course2023.getId())))
        .thenReturn(List.of(exam2023));
    when(gradeRepository.findByExamIdIn(List.of(exam2023.getId())))
        .thenReturn(List.of(grade(s1, exam2023.getId(), 12.0)));
    when(graduateCalculator.passingAverage(s1, null)).thenReturn(Optional.of(12.0));

    PromotionResultsResponse response = service.getPromotionResults(promotionId);

    assertThat(response.year1().average()).isEqualTo(12.0);
    assertThat(response.year1().bestStudentId()).isEqualTo(s1);
    assertThat(response.year1().failureRate()).isEqualTo(0.0);
    assertThat(response.year2().average()).isNull();
    assertThat(response.year3().average()).isNull();
    assertThat(response.overallAverage()).isEqualTo(12.0);
    assertThat(response.graduationRate()).isEqualTo(100.0);
  }

  @Test
  void promotion_without_students_has_zero_graduation_rate() {
    UUID promotionId = UUID.randomUUID();
    JPromotion promotion = promotion(promotionId, 2023);
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(groupRepository.findByPromotionId(promotionId)).thenReturn(List.of());

    PromotionResultsResponse response = service.getPromotionResults(promotionId);

    assertThat(response.year1().average()).isNull();
    assertThat(response.overallAverage()).isNull();
    assertThat(response.graduationRate()).isEqualTo(0.0);
  }

  @Test
  void unknown_promotion_throws_not_found() {
    UUID promotionId = UUID.randomUUID();
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPromotionResults(promotionId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Promotion not found");
  }

  private static JPromotion promotion(UUID id, int year) {
    JPromotion promotion = new JPromotion();
    promotion.setId(id);
    promotion.setRef("P-" + year);
    promotion.setYear(year);
    return promotion;
  }

  private static JGroup group(UUID promotionId) {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP-" + UUID.randomUUID());
    group.setPromotionId(promotionId);
    return group;
  }

  private static JStudentGroup membership(UUID studentId, UUID groupId) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(studentId);
    membership.setGroupId(groupId);
    membership.setStartDate(Instant.parse("2023-09-01T08:00:00Z"));
    membership.setEndDate(null);
    return membership;
  }

  private static JCourse course(UUID promotionId) {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle("Maths");
    course.setCredits(6);
    course.setPromotionId(promotionId);
    return course;
  }

  private static JExam exam(UUID courseId) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(courseId);
    exam.setDateExam(Instant.parse("2023-11-15T09:00:00Z"));
    exam.setCoefficient(1.5);
    return exam;
  }

  private static JGrade grade(UUID studentId, UUID examId, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(studentId);
    grade.setExamId(examId);
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return grade;
  }
}
