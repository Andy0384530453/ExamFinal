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
    UUID userId = authenticate(jwt);
    if (isAdmin(jwt)) {
      return;
    }
    if (!isTeacherOfCourse(userId, courseId)) {
      throw new AccessDeniedException("A teacher can only access grades of their own courses");
    }
  }

  public void checkAdminOrTeacherOfGrade(UUID gradeId, Jwt jwt) {
    UUID userId = authenticate(jwt);
    if (isAdmin(jwt)) {
      return;
    }
    UUID courseId = courseIdOfGrade(gradeId);
    if (!isTeacherOfCourse(userId, courseId)) {
      throw new AccessDeniedException("A teacher can only access grades of their own courses");
    }
  }

  private UUID authenticate(Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (Role.ADMIN.name().equals(role) || Role.TEACHER.name().equals(role)) {
      return UUID.fromString(tokenProvider.getUserId(jwt));
    }
    throw new AccessDeniedException("Access denied: insufficient role");
  }

  private boolean isAdmin(Jwt jwt) {
    return Role.ADMIN.name().equals(tokenProvider.getRole(jwt));
  }

  private boolean isTeacherOfCourse(UUID teacherId, UUID courseId) {
    return courseTeacherRepository.findByTeacherId(teacherId).stream()
        .anyMatch(courseTeacher -> courseTeacher.getCourseId().equals(courseId));
  }

  private UUID courseIdOfGrade(UUID gradeId) {
    JGrade grade =
        gradeRepository
            .findById(gradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
    JExam exam =
        examRepository
            .findById(grade.getExamId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Exam not found with id: " + grade.getExamId()));
    return exam.getCourseId();
  }
}