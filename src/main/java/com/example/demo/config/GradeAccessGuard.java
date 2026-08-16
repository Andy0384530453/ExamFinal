package com.example.demo.config;

import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class GradeAccessGuard {

  private final TokenProvider tokenProvider;
  private final JGradeRepository gradeRepository;
  private final JExamRepository examRepository;
  private final JCourseTeacherRepository courseTeacherRepository;

  public GradeAccessGuard(
      TokenProvider tokenProvider,
      JGradeRepository gradeRepository,
      JExamRepository examRepository,
      JCourseTeacherRepository courseTeacherRepository) {
    this.tokenProvider = tokenProvider;
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseTeacherRepository = courseTeacherRepository;
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
    JGrade grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
    if (isAdmin(jwt)) {
      return grade;
    }
    UUID teacherId = userId(jwt);
    if (!isTeacherOfCourse(teacherId, courseIdOfGrade(grade))) {
      throw new AccessDeniedException("A teacher can only access grades of their own courses");
    }
    return grade;
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
    JExam exam =
        examRepository
            .findById(grade.getExamId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Exam not found with id: " + grade.getExamId()));
    return exam.getCourseId();
  }
}
