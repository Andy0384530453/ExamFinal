package com.example.demo.config;

import com.example.demo.entity.JGrade;
import com.example.demo.enums.Role;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.validator.EntityValidator;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class GradeAccessGuard {

  private final TokenProvider tokenProvider;
  private final AccessGuard accessGuard;
  private final JCourseTeacherRepository courseTeacherRepository;
  private final EntityValidator validator;

  public GradeAccessGuard(
      TokenProvider tokenProvider,
      AccessGuard accessGuard,
      JCourseTeacherRepository courseTeacherRepository,
      EntityValidator validator) {
    this.tokenProvider = tokenProvider;
    this.accessGuard = accessGuard;
    this.courseTeacherRepository = courseTeacherRepository;
    this.validator = validator;
  }

  public void checkAdminOrTeacherOfCourse(UUID courseId, Jwt jwt) {
    requireAdminOrTeacher(jwt);
    if (isAdmin(jwt)) {
      return;
    }
    UUID teacherId = userId(jwt);
    if (!isTeacherOfCourse(teacherId, courseId)) {
      throw new AccessDeniedException("A teacher can only access grades of their own courses");
    }
  }

  public JGrade checkAdminOrTeacherOfGrade(UUID gradeId, Jwt jwt) {
    requireAdminOrTeacher(jwt);
    JGrade grade = validator.requireGrade(gradeId);
    if (isAdmin(jwt)) {
      return grade;
    }
    UUID teacherId = userId(jwt);
    if (!isTeacherOfCourse(teacherId, courseIdOfGrade(grade))) {
      throw new AccessDeniedException("A teacher can only access grades of their own courses");
    }
    return grade;
  }

  public void checkAdminOrStudentSelf(UUID studentId, Jwt jwt) {
    accessGuard.checkAdminOrStudentSelf(
        studentId, jwt, "A student can only access their own grades");
  }

  private void requireAdminOrTeacher(Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (!Role.ADMIN.name().equals(role) && !Role.TEACHER.name().equals(role)) {
      throw new AccessDeniedException("Access denied: insufficient role");
    }
  }

  private boolean isAdmin(Jwt jwt) {
    return Role.ADMIN.name().equals(tokenProvider.getRole(jwt));
  }

  private UUID userId(Jwt jwt) {
    return UUID.fromString(tokenProvider.getUserId(jwt));
  }

  private boolean isTeacherOfCourse(UUID teacherId, UUID courseId) {
    return courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId);
  }

  private UUID courseIdOfGrade(JGrade grade) {
    return validator.requireExam(grade.getExamId()).getCourseId();
  }
}
