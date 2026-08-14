package com.example.demo.service;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptItemResponse;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.entity.Course;
import com.example.demo.entity.Exam;
import com.example.demo.entity.Grade;
import com.example.demo.entity.Transcript;
import com.example.demo.entity.TranscriptItem;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TranscriptMapper;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.GradeRepository;
import com.example.demo.repository.PromotionRepository;
import com.example.demo.repository.TranscriptRepository;
import com.example.demo.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TranscriptServiceImpl implements TranscriptService {

  private final TokenProvider tokenProvider;
  private final UserRepository userRepository;
  private final PromotionRepository promotionRepository;
  private final GradeRepository gradeRepository;
  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;
  private final TranscriptRepository transcriptRepository;
  private final TranscriptMapper transcriptMapper;

  public TranscriptServiceImpl(
      TokenProvider tokenProvider,
      UserRepository userRepository,
      PromotionRepository promotionRepository,
      GradeRepository gradeRepository,
      ExamRepository examRepository,
      CourseRepository courseRepository,
      TranscriptRepository transcriptRepository,
      TranscriptMapper transcriptMapper) {
    this.tokenProvider = tokenProvider;
    this.userRepository = userRepository;
    this.promotionRepository = promotionRepository;
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseRepository = courseRepository;
    this.transcriptRepository = transcriptRepository;
    this.transcriptMapper = transcriptMapper;
  }

  @Override
  public TranscriptResponse getStudentTranscript(UUID studentId, UUID promotionId, Jwt jwt) {
    checkAccess(studentId, jwt);
    checkStudentExists(studentId);
    checkPromotionExists(promotionId);

    List<TranscriptItem> items = buildItems(studentId, promotionId);
    Transcript transcript = resolveTranscript(studentId, promotionId);
    List<TranscriptItemResponse> itemResponses =
        items.stream().map(transcriptMapper::toItemResponse).toList();
    return transcriptMapper.toResponse(transcript, itemResponses);
  }

  private void checkAccess(UUID studentId, Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (Role.ADMIN.name().equals(role)) {
      return;
    }
    if (Role.STUDENT.name().equals(role)) {
      UUID authenticatedId = UUID.fromString(tokenProvider.getUserId(jwt));
      if (!authenticatedId.equals(studentId)) {
        throw new AccessDeniedException("A student can only access their own transcript");
      }
      return;
    }
    throw new AccessDeniedException("Access denied: insufficient role");
  }

  private void checkStudentExists(UUID studentId) {
    Optional<User> student = userRepository.findById(studentId);
    if (student.isEmpty() || student.get().getRole() != Role.STUDENT) {
      throw new ResourceNotFoundException("Student not found with id: " + studentId);
    }
  }

  private void checkPromotionExists(UUID promotionId) {
    if (promotionId != null && !promotionRepository.existsById(promotionId)) {
      throw new ResourceNotFoundException("Promotion not found with id: " + promotionId);
    }
  }

  private List<TranscriptItem> buildItems(UUID studentId, UUID promotionId) {
    return gradeRepository.findByStudentId(studentId).stream()
        .flatMap(grade -> toTranscriptItem(grade, promotionId).stream())
        .sorted(Comparator.comparing(TranscriptItem::getExamDate))
        .toList();
  }

  private Optional<TranscriptItem> toTranscriptItem(Grade grade, UUID promotionId) {
    Optional<Exam> exam = examRepository.findById(grade.getExamId());
    if (exam.isEmpty()) {
      return Optional.empty();
    }
    Optional<Course> course = courseRepository.findById(exam.get().getCourseId());
    if (course.isEmpty()) {
      return Optional.empty();
    }
    Course courseValue = course.get();
    if (promotionId != null && !promotionId.equals(courseValue.getPromotionId())) {
      return Optional.empty();
    }
    TranscriptItem item = new TranscriptItem();
    item.setCourseTitle(courseValue.getTitle());
    item.setExamDate(exam.get().getDateExam());
    item.setCoefficient(exam.get().getCoefficient());
    item.setGrade(grade.getValue());
    item.setCredits(courseValue.getCredits());
    return Optional.of(item);
  }

  private Transcript resolveTranscript(UUID studentId, UUID promotionId) {
    Optional<Transcript> persisted =
        promotionId == null
            ? transcriptRepository.findByStudentIdAndPromotionIdIsNull(studentId)
            : transcriptRepository.findByStudentIdAndPromotionId(studentId, promotionId);
    if (persisted.isPresent()) {
      return persisted.get();
    }
    Transcript transcript = new Transcript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(studentId);
    transcript.setPromotionId(promotionId);
    transcript.setStatus(TranscriptStatus.PENDING);
    return transcript;
  }
}
