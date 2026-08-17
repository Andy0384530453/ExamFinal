package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGradeModification;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeModificationRepository;
import com.example.demo.repository.JGradeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

class GradeServiceImplTest {

  private GradeAccessGuard accessGuard;
  private TokenProvider tokenProvider;
  private JGradeRepository gradeRepository;
  private JExamRepository examRepository;
  private JCourseRepository courseRepository;
  private JGradeModificationRepository gradeModificationRepository;
  private GradeServiceImpl service;

  @BeforeEach
  void setUp() {
    accessGuard = mock(GradeAccessGuard.class);
    tokenProvider = mock(TokenProvider.class);
    gradeRepository = mock(JGradeRepository.class);
    examRepository = mock(JExamRepository.class);
    courseRepository = mock(JCourseRepository.class);
    gradeModificationRepository = mock(JGradeModificationRepository.class);
    service =
        new GradeServiceImpl(
            accessGuard,
            tokenProvider,
            gradeRepository,
            examRepository,
            courseRepository,
            gradeModificationRepository);
  }

  @Test
  void list_grades_for_course_returns_mapped_grades() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID examId = UUID.randomUUID();
    JExam exam = exam(examId, courseId);
    JGrade grade = grade(examId, 14.5);
    when(examRepository.findByCourseIdIn(List.of(courseId))).thenReturn(List.of(exam));
    when(gradeRepository.findByExamIdIn(List.of(examId))).thenReturn(List.of(grade));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, "Maths")));

    List<GradeResponse> grades = service.listGradesForCourse(courseId, jwt);

    assertThat(grades).hasSize(1);
    GradeResponse response = grades.get(0);
    assertThat(response.id()).isEqualTo(grade.getId());
    assertThat(response.studentId()).isEqualTo(grade.getStudentId());
    assertThat(response.examId()).isEqualTo(examId);
    assertThat(response.examRef()).isEqualTo(exam.getRef());
    assertThat(response.courseTitle()).isEqualTo("Maths");
    assertThat(response.value()).isEqualTo(14.5);
    verify(accessGuard).checkAdminOrTeacherOfCourse(courseId, jwt);
  }

  @Test
  void list_grades_for_course_without_exams_returns_empty() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    when(examRepository.findByCourseIdIn(List.of(courseId))).thenReturn(List.of());

    List<GradeResponse> grades = service.listGradesForCourse(courseId, jwt);

    assertThat(grades).isEmpty();
  }

  @Test
  void list_grades_for_student_returns_mapped_grades() {
    Jwt jwt = mock(Jwt.class);
    UUID studentId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID examId = UUID.randomUUID();
    JExam exam = exam(examId, courseId);
    JGrade grade = grade(examId, 13.0);
    grade.setStudentId(studentId);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));
    when(examRepository.findAllById(List.of(examId))).thenReturn(List.of(exam));
    when(courseRepository.findAllById(List.of(courseId)))
        .thenReturn(List.of(course(courseId, "Maths")));

    List<GradeResponse> grades = service.listGradesForStudent(studentId, jwt);

    assertThat(grades).hasSize(1);
    GradeResponse response = grades.get(0);
    assertThat(response.id()).isEqualTo(grade.getId());
    assertThat(response.studentId()).isEqualTo(studentId);
    assertThat(response.examId()).isEqualTo(examId);
    assertThat(response.examRef()).isEqualTo(exam.getRef());
    assertThat(response.courseTitle()).isEqualTo("Maths");
    assertThat(response.value()).isEqualTo(13.0);
    verify(accessGuard).checkAdminOrStudentSelf(studentId, jwt);
  }

  @Test
  void list_grades_for_student_without_grades_returns_empty() {
    Jwt jwt = mock(Jwt.class);
    UUID studentId = UUID.randomUUID();
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    List<GradeResponse> grades = service.listGradesForStudent(studentId, jwt);

    assertThat(grades).isEmpty();
  }

  @Test
  void update_grade_writes_history_and_updates_value() {
    Jwt jwt = mock(Jwt.class);
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID examId = UUID.randomUUID();
    UUID gradeId = UUID.randomUUID();
    JGrade grade = grade(examId, 10.0);
    JExam exam = exam(examId, courseId);
    when(accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt)).thenReturn(grade);
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, "Maths")));
    when(tokenProvider.getUserId(jwt)).thenReturn(userId.toString());

    GradeResponse response =
        service.updateGrade(gradeId, new GradeUpdateRequest(14.5, "Claim"), jwt);

    assertThat(response.value()).isEqualTo(14.5);
    assertThat(response.examId()).isEqualTo(examId);
    assertThat(response.examRef()).isEqualTo(exam.getRef());
    assertThat(response.courseTitle()).isEqualTo("Maths");
    verify(gradeRepository).save(grade);
    assertThat(grade.getValue()).isEqualTo(14.5);
    assertThat(grade.getModifiedBy()).isEqualTo(userId);
    assertThat(grade.getModifiedAt()).isNotNull();

    ArgumentCaptor<JGradeModification> captor = ArgumentCaptor.forClass(JGradeModification.class);
    verify(gradeModificationRepository).save(captor.capture());
    JGradeModification modification = captor.getValue();
    assertThat(modification.getGradeId()).isEqualTo(gradeId);
    assertThat(modification.getOldValue()).isEqualTo(10.0);
    assertThat(modification.getNewValue()).isEqualTo(14.5);
    assertThat(modification.getReason()).isEqualTo("Claim");
    assertThat(modification.getModifiedBy()).isEqualTo(userId);
    assertThat(modification.getModifiedAt()).isNotNull();
  }

  @Test
  void update_grade_with_same_value_throws_illegal_argument() {
    Jwt jwt = mock(Jwt.class);
    UUID examId = UUID.randomUUID();
    UUID gradeId = UUID.randomUUID();
    JGrade grade = grade(examId, 10.0);
    when(accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt)).thenReturn(grade);

    assertThatThrownBy(
            () -> service.updateGrade(gradeId, new GradeUpdateRequest(10.0, "Claim"), jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already has value");

    verify(gradeModificationRepository, never()).save(any());
  }

  @Test
  void update_unknown_grade_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID gradeId = UUID.randomUUID();
    when(accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt))
        .thenThrow(new ResourceNotFoundException("Grade not found with id: " + gradeId));

    assertThatThrownBy(
            () -> service.updateGrade(gradeId, new GradeUpdateRequest(14.5, "Claim"), jwt))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void get_history_returns_mapped_modifications() {
    Jwt jwt = mock(Jwt.class);
    UUID gradeId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    JGradeModification modification = new JGradeModification();
    modification.setId(UUID.randomUUID());
    modification.setGradeId(gradeId);
    modification.setOldValue(10.0);
    modification.setNewValue(14.5);
    modification.setReason("Claim");
    modification.setModifiedAt(Instant.parse("2024-01-01T10:00:00Z"));
    modification.setModifiedBy(userId);
    when(gradeModificationRepository.findByGradeIdOrderByModifiedAtDesc(gradeId))
        .thenReturn(List.of(modification));

    List<GradeHistoryResponse> history = service.getGradeHistory(gradeId, jwt);

    assertThat(history).hasSize(1);
    GradeHistoryResponse response = history.get(0);
    assertThat(response.oldValue()).isEqualTo(10.0);
    assertThat(response.newValue()).isEqualTo(14.5);
    assertThat(response.reason()).isEqualTo("Claim");
    assertThat(response.modifiedBy()).isEqualTo(userId);
    verify(accessGuard).checkAdminOrTeacherOfGrade(gradeId, jwt);
  }

  private static JCourse course(UUID courseId, String title) {
    JCourse course = new JCourse();
    course.setId(courseId);
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(6);
    course.setPromotionId(UUID.randomUUID());
    return course;
  }

  private static JExam exam(UUID examId, UUID courseId) {
    JExam exam = new JExam();
    exam.setId(examId);
    exam.setCourseId(courseId);
    exam.setRef("EX-" + UUID.randomUUID());
    return exam;
  }

  private static JGrade grade(UUID examId, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(UUID.randomUUID());
    grade.setExamId(examId);
    grade.setValue(value);
    grade.setModifiedAt(Instant.parse("2024-01-01T09:00:00Z"));
    grade.setModifiedBy(UUID.randomUUID());
    return grade;
  }
}
