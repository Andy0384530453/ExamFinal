package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.JCourseTeacher;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class GradeAccessGuardTest {

  private TokenProvider tokenProvider;
  private JGradeRepository gradeRepository;
  private JExamRepository examRepository;
  private JCourseTeacherRepository courseTeacherRepository;
  private GradeAccessGuard guard;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    gradeRepository = mock(JGradeRepository.class);
    examRepository = mock(JExamRepository.class);
    courseTeacherRepository = mock(JCourseTeacherRepository.class);
    guard =
        new GradeAccessGuard(
            tokenProvider, gradeRepository, examRepository, courseTeacherRepository);
  }

  @Test
  void admin_can_access_any_course() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());

    assertThatCode(() -> guard.checkAdminOrTeacherOfCourse(UUID.randomUUID(), jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void teacher_can_access_own_course() {
    UUID teacherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(courseTeacherRepository.findByTeacherId(teacherId))
        .thenReturn(List.of(courseTeacher(courseId)));

    assertThatCode(() -> guard.checkAdminOrTeacherOfCourse(courseId, jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void teacher_cannot_access_another_teacher_course() {
    UUID teacherId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(courseTeacherRepository.findByTeacherId(teacherId)).thenReturn(List.of());

    assertThatThrownBy(() -> guard.checkAdminOrTeacherOfCourse(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void teacher_can_access_grade_of_own_course() {
    UUID teacherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID examId = UUID.randomUUID();
    UUID gradeId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(examId)));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam(courseId)));
    when(courseTeacherRepository.findByTeacherId(teacherId))
        .thenReturn(List.of(courseTeacher(courseId)));

    assertThatCode(() -> guard.checkAdminOrTeacherOfGrade(gradeId, jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void teacher_cannot_access_grade_of_another_course() {
    UUID teacherId = UUID.randomUUID();
    UUID examId = UUID.randomUUID();
    UUID gradeId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade(examId)));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam(UUID.randomUUID())));
    when(courseTeacherRepository.findByTeacherId(teacherId)).thenReturn(List.of());

    assertThatThrownBy(() -> guard.checkAdminOrTeacherOfGrade(gradeId, jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void teacher_with_unknown_grade_throws_not_found() {
    UUID teacherId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(gradeRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> guard.checkAdminOrTeacherOfGrade(UUID.randomUUID(), jwt))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void student_cannot_access_grades() {
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());

    assertThatThrownBy(() -> guard.checkAdminOrTeacherOfCourse(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static Jwt jwt(Role role) {
    return jwt(role, UUID.randomUUID());
  }

  private static Jwt jwt(Role role, UUID userId) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(userId.toString())
        .claim("role", role.name())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .build();
  }

  private static JCourseTeacher courseTeacher(UUID courseId) {
    JCourseTeacher courseTeacher = new JCourseTeacher();
    courseTeacher.setId(UUID.randomUUID());
    courseTeacher.setCourseId(courseId);
    courseTeacher.setTeacherId(UUID.randomUUID());
    return courseTeacher;
  }

  private static JExam exam(UUID courseId) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setCourseId(courseId);
    return exam;
  }

  private static JGrade grade(UUID examId) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(UUID.randomUUID());
    grade.setExamId(examId);
    grade.setValue(12.0);
    return grade;
  }
}