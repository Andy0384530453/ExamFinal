package com.example.demo.service;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeServiceImpl implements GradeService {

  private final GradeAccessGuard accessGuard;
  private final TokenProvider tokenProvider;
  private final JGradeRepository gradeRepository;
  private final JExamRepository examRepository;
  private final JCourseRepository courseRepository;
  private final JGradeModificationRepository gradeModificationRepository;

  public GradeServiceImpl(
      GradeAccessGuard accessGuard,
      TokenProvider tokenProvider,
      JGradeRepository gradeRepository,
      JExamRepository examRepository,
      JCourseRepository courseRepository,
      JGradeModificationRepository gradeModificationRepository) {
    this.accessGuard = accessGuard;
    this.tokenProvider = tokenProvider;
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseRepository = courseRepository;
    this.gradeModificationRepository = gradeModificationRepository;
  }

  @Override
  public List<GradeResponse> listGradesForCourse(UUID courseId, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    List<JExam> exams = examRepository.findByCourseIdIn(List.of(courseId));
    List<UUID> examIds = exams.stream().map(JExam::getId).toList();
    if (examIds.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> examRefs =
        exams.stream().collect(Collectors.toMap(JExam::getId, JExam::getRef));
    String courseTitle = courseTitle(courseId);
    return gradeRepository.findByExamIdIn(examIds).stream()
        .map(
            grade ->
                toResponse(grade, courseTitle, grade.getExamId(), examRefs.get(grade.getExamId())))
        .toList();
  }

  @Override
  @Transactional
  public GradeResponse updateGrade(UUID gradeId, GradeUpdateRequest request, Jwt jwt) {
    JGrade grade = accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt);
    if (Double.compare(grade.getValue(), request.value()) == 0) {
      throw new IllegalArgumentException("Grade already has value " + request.value());
    }
    JExam exam = requireExam(grade.getExamId());
    String courseTitle = courseTitle(exam.getCourseId());
    UUID userId = UUID.fromString(tokenProvider.getUserId(jwt));
    Instant now = Instant.now();

    JGradeModification modification = new JGradeModification();
    modification.setId(UUID.randomUUID());
    modification.setGradeId(gradeId);
    modification.setOldValue(grade.getValue());
    modification.setNewValue(request.value());
    modification.setReason(request.reason());
    modification.setModifiedAt(now);
    modification.setModifiedBy(userId);
    gradeModificationRepository.save(modification);

    grade.setValue(request.value());
    grade.setModifiedAt(now);
    grade.setModifiedBy(userId);
    gradeRepository.save(grade);

    return toResponse(grade, courseTitle, exam.getId(), exam.getRef());
  }

  @Override
  public List<GradeHistoryResponse> getGradeHistory(UUID gradeId, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt);
    return gradeModificationRepository.findByGradeIdOrderByModifiedAtDesc(gradeId).stream()
        .map(
            m ->
                new GradeHistoryResponse(
                    m.getOldValue(),
                    m.getNewValue(),
                    m.getReason(),
                    m.getModifiedAt(),
                    m.getModifiedBy()))
        .toList();
  }

  private GradeResponse toResponse(JGrade grade, String courseTitle, UUID examId, String examRef) {
    return new GradeResponse(
        grade.getId(),
        grade.getStudentId(),
        examId,
        examRef,
        courseTitle,
        grade.getValue(),
        grade.getComment(),
        grade.getModifiedAt(),
        grade.getModifiedBy());
  }

  private String courseTitle(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .map(JCourse::getTitle)
        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
  }

  private JExam requireExam(UUID examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
  }
}
