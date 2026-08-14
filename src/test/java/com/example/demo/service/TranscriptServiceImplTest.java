package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.entity.Course;
import com.example.demo.entity.Exam;
import com.example.demo.entity.Grade;
import com.example.demo.entity.Promotion;
import com.example.demo.entity.Transcript;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class TranscriptServiceImplTest {

  private TokenProvider tokenProvider;
  private UserRepository userRepository;
  private PromotionRepository promotionRepository;
  private GradeRepository gradeRepository;
  private ExamRepository examRepository;
  private CourseRepository courseRepository;
  private TranscriptRepository transcriptRepository;
  private TranscriptServiceImpl service;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    userRepository = mock(UserRepository.class);
    promotionRepository = mock(PromotionRepository.class);
    gradeRepository = mock(GradeRepository.class);
    examRepository = mock(ExamRepository.class);
    courseRepository = mock(CourseRepository.class);
    transcriptRepository = mock(TranscriptRepository.class);
    service =
        new TranscriptServiceImpl(
            tokenProvider,
            userRepository,
            promotionRepository,
            gradeRepository,
            examRepository,
            courseRepository,
            transcriptRepository,
            new TranscriptMapper());
  }

  @Test
  void admin_can_access_any_student_transcript() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of());

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.studentId()).isEqualTo(student.getId());
    assertThat(response.items()).isEmpty();
    assertThat(response.status()).isEqualTo(TranscriptStatus.PENDING);
  }

  @Test
  void student_can_access_own_transcript() {
    User student = student();
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(student.getId().toString());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of());

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.studentId()).isEqualTo(student.getId());
  }

  @Test
  void student_cannot_access_another_student_transcript() {
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(UUID.randomUUID().toString());

    assertThatThrownBy(() -> service.getStudentTranscript(UUID.randomUUID(), null, jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void teacher_cannot_access_transcript() {
    Jwt jwt = jwt(Role.TEACHER);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());

    assertThatThrownBy(() -> service.getStudentTranscript(UUID.randomUUID(), null, jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void unknown_student_throws_not_found() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getStudentTranscript(UUID.randomUUID(), null, jwt))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void unknown_promotion_throws_not_found() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(promotionRepository.existsById(any(UUID.class))).thenReturn(false);

    assertThatThrownBy(() -> service.getStudentTranscript(student.getId(), UUID.randomUUID(), jwt))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void without_promotion_transcript_contains_all_grades() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    Grade maths = gradeFor(student, promotion(), "Maths", 6, "2023-01-01T09:00:00Z", 12.0);
    Grade physique = gradeFor(student, promotion(), "Physique", 5, "2024-01-01T09:00:00Z", 14.0);
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of(maths, physique));

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items()).extracting("courseTitle").containsExactly("Maths", "Physique");
    assertThat(response.promotionId()).isNull();
  }

  @Test
  void with_promotion_transcript_is_filtered() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    Promotion promotionA = promotion();
    Promotion promotionB = promotion();
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(promotionRepository.existsById(promotionA.getId())).thenReturn(true);
    Grade maths = gradeFor(student, promotionA, "Maths", 6, "2023-01-01T09:00:00Z", 12.0);
    Grade physique = gradeFor(student, promotionB, "Physique", 5, "2024-01-01T09:00:00Z", 14.0);
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of(maths, physique));

    TranscriptResponse response =
        service.getStudentTranscript(student.getId(), promotionA.getId(), jwt);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).courseTitle()).isEqualTo("Maths");
    assertThat(response.promotionId()).isEqualTo(promotionA.getId());
  }

  @Test
  void multiple_grades_produce_multiple_items_in_date_order() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    Grade later = gradeFor(student, promotion(), "B-Course", 6, "2024-01-01T09:00:00Z", 11.0);
    Grade earlier = gradeFor(student, promotion(), "A-Course", 5, "2023-01-01T09:00:00Z", 15.0);
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of(later, earlier));

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items()).extracting("courseTitle").containsExactly("A-Course", "B-Course");
  }

  @Test
  void reuses_persisted_transcript_information() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    Transcript persisted = new Transcript();
    persisted.setId(UUID.randomUUID());
    persisted.setStudentId(student.getId());
    persisted.setPromotionId(null);
    persisted.setStatus(TranscriptStatus.GENERATED);
    persisted.setPdfUrl("https://s3.example/transcript.pdf");
    persisted.setEmail("student@school.com");
    persisted.setGeneratedAt(Instant.parse("2024-01-01T10:00:00Z"));
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(gradeRepository.findByStudentId(student.getId())).thenReturn(List.of());
    when(transcriptRepository.findByStudentIdAndPromotionIdIsNull(student.getId()))
        .thenReturn(Optional.of(persisted));

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.id()).isEqualTo(persisted.getId());
    assertThat(response.status()).isEqualTo(TranscriptStatus.GENERATED);
    assertThat(response.pdfUrl()).isEqualTo("https://s3.example/transcript.pdf");
    assertThat(response.email()).isEqualTo("student@school.com");
    assertThat(response.generatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
  }

  private Grade gradeFor(
      User student, Promotion promotion, String title, int credits, String date, double value) {
    Course course = course(promotion.getId(), title, credits);
    Exam exam = exam(course, date);
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
    return grade(student, exam, value);
  }

  private static Jwt jwt(Role role) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .claim("role", role.name())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .build();
  }

  private static User student() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(Role.STUDENT);
    return user;
  }

  private static Promotion promotion() {
    Promotion promotion = new Promotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + UUID.randomUUID());
    promotion.setYear(2024);
    return promotion;
  }

  private static Course course(UUID promotionId, String title, int credits) {
    Course course = new Course();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotionId);
    return course;
  }

  private static Exam exam(Course course, String date) {
    Exam exam = new Exam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(1.0);
    return exam;
  }

  private static Grade grade(User student, Exam exam, double value) {
    Grade grade = new Grade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return grade;
  }
}
