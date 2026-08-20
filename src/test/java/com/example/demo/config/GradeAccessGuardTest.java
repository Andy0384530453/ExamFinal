package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.validator.EntityValidator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class GradeAccessGuardTest {

  private TokenProvider tokenProvider;
  private JExamRepository examRepository;
  private JGradeRepository gradeRepository;
  private JCourseTeacherRepository courseTeacherRepository;
  private GradeAccessGuard guard;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    examRepository = mock(JExamRepository.class);
    gradeRepository = mock(JGradeRepository.class);
    courseTeacherRepository = mock(JCourseTeacherRepository.class);
    EntityValidator validator =
        new EntityValidator(null, null, null, null, examRepository, gradeRepository, null);
    guard = new GradeAccessGuard(tokenProvider, courseTeacherRepository, validator);
  }

  @Test
  void admin_can_access_any_course() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());

    assertThatCode(() -> guard.checkAdminOrTeacherOfCourse(UUID.randomUUID(), jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void admin_can_access_any_grade_and_returns_it() {
    UUID gradeId = UUID.randomUUID();
    JGrade grade = grade(UUID.randomUUID());
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));

    JGrade returned = guard.checkAdminOrTeacherOfGrade(gradeId, jwt);

    assertThat(returned).isEqualTo(grade);
  }

  @Test
  void teacher_can_access_own_course() {
    UUID teacherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);

    assertThatCode(() -> guard.checkAdminOrTeacherOfCourse(courseId, jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void teacher_cannot_access_another_teacher_course() {
    UUID teacherId = UUID.randomUUID();
    Jwt jwt = jwt(Role.TEACHER, teacherId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(any(UUID.class), any(UUID.class)))
        .thenReturn(false);

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
    JGrade grade = grade(examId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(teacherId.toString());
    when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam(courseId)));
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);

    JGrade returned = guard.checkAdminOrTeacherOfGrade(gradeId, jwt);

    assertThat(returned).isEqualTo(grade);
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
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(any(UUID.class), any(UUID.class)))
        .thenReturn(false);

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

  @Test
  void admin_can_access_any_student_grades() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());

    assertThatCode(() -> guard.checkAdminOrStudentSelf(UUID.randomUUID(), jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void student_can_access_own_grades() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.STUDENT, studentId);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(studentId.toString());

    assertThatCode(() -> guard.checkAdminOrStudentSelf(studentId, jwt)).doesNotThrowAnyException();
  }

  @Test
  void student_cannot_access_another_student_grades() {
    Jwt jwt = jwt(Role.STUDENT, UUID.randomUUID());
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(UUID.randomUUID().toString());

    assertThatThrownBy(() -> guard.checkAdminOrStudentSelf(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("own grades");
  }

  @Test
  void teacher_cannot_access_student_grades() {
    Jwt jwt = jwt(Role.TEACHER);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());

    assertThatThrownBy(() -> guard.checkAdminOrStudentSelf(UUID.randomUUID(), jwt))
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
