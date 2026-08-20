package com.example.demo.service;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.grade.GradeCreateRequest;
import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGradeModification;
import com.example.demo.exception.ConflictException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeModificationRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.validator.EntityValidator;
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
  private final EntityValidator validator;

  public GradeServiceImpl(
      GradeAccessGuard accessGuard,
      TokenProvider tokenProvider,
      JGradeRepository gradeRepository,
      JExamRepository examRepository,
      JCourseRepository courseRepository,
      JGradeModificationRepository gradeModificationRepository,
      EntityValidator validator) {
    this.accessGuard = accessGuard;
    this.tokenProvider = tokenProvider;
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseRepository = courseRepository;
    this.gradeModificationRepository = gradeModificationRepository;
    this.validator = validator;
  }

  @Override
  @Transactional
  public GradeResponse createGrade(UUID courseId, GradeCreateRequest request, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    validator.assertStudentExists(request.studentId());
    JExam exam = validator.requireExam(request.examId());
    if (!courseId.equals(exam.getCourseId())) {
      throw new IllegalArgumentException(
          "Exam " + exam.getId() + " does not belong to course " + courseId);
    }
    if (gradeRepository
        .findByStudentIdAndExamId(request.studentId(), request.examId())
        .isPresent()) {
      throw new ConflictException(
          "A grade already exists for student "
              + request.studentId()
              + " on exam "
              + request.examId());
    }
    UUID userId = UUID.fromString(tokenProvider.getUserId(jwt));
    Instant now = Instant.now();

    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(request.studentId());
    grade.setExamId(request.examId());
    grade.setValue(request.value());
    grade.setComment(request.comment());
    grade.setModifiedAt(now);
    grade.setModifiedBy(userId);
    gradeRepository.save(grade);

    String courseTitle = validator.requireCourse(courseId).getTitle();
    return toResponse(grade, courseTitle, exam.getId(), exam.getRef());
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
    String courseTitle = validator.requireCourse(courseId).getTitle();
    return gradeRepository.findByExamIdIn(examIds).stream()
        .map(
            grade ->
                toResponse(grade, courseTitle, grade.getExamId(), examRefs.get(grade.getExamId())))
        .toList();
  }

  @Override
  public List<GradeResponse> listGradesForStudent(UUID studentId, Jwt jwt) {
    accessGuard.checkAdminOrStudentSelf(studentId, jwt);
    List<JGrade> grades = gradeRepository.findByStudentId(studentId);
    if (grades.isEmpty()) {
      return List.of();
    }
    List<UUID> examIds = grades.stream().map(JGrade::getExamId).distinct().toList();
    Map<UUID, JExam> examsById =
        examRepository.findAllById(examIds).stream()
            .collect(Collectors.toMap(JExam::getId, exam -> exam));
    List<UUID> courseIds = examsById.values().stream().map(JExam::getCourseId).distinct().toList();
    Map<UUID, String> courseTitles =
        courseRepository.findAllById(courseIds).stream()
            .collect(Collectors.toMap(JCourse::getId, JCourse::getTitle));
    return grades.stream()
        .map(
            grade -> {
              JExam exam = examsById.get(grade.getExamId());
              return toResponse(
                  grade, courseTitles.get(exam.getCourseId()), grade.getExamId(), exam.getRef());
            })
        .toList();
  }

  @Override
  @Transactional
  public GradeResponse updateGrade(UUID gradeId, GradeUpdateRequest request, Jwt jwt) {
    JGrade grade = accessGuard.checkAdminOrTeacherOfGrade(gradeId, jwt);
    if (Double.compare(grade.getValue(), request.value()) == 0) {
      throw new IllegalArgumentException("Grade already has value " + request.value());
    }
    JExam exam = validator.requireExam(grade.getExamId());
    String courseTitle = validator.requireCourse(exam.getCourseId()).getTitle();
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
}
