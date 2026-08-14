package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
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
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.GradeRepository;
import com.example.demo.repository.PromotionRepository;
import com.example.demo.repository.TranscriptRepository;
import com.example.demo.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class TranscriptControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private TranscriptRepository transcriptRepository;

  @Test
  void admin_can_access_any_student_transcript() {
    User student = student();
    Promotion promotion = promotion();
    Course course = course(promotion, "Mathematiques", 6);
    Exam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);
    User admin = admin();

    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(admin), student.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TranscriptResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.promotionId()).isNull();
    assertThat(body.status()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(body.items()).hasSize(1);
    assertThat(body.items().get(0).courseTitle()).isEqualTo("Mathematiques");
    assertThat(body.items().get(0).grade()).isEqualTo(14.5);
    assertThat(body.items().get(0).credits()).isEqualTo(6);
  }

  @Test
  void student_can_access_own_transcript() {
    User student = student();
    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(student), student.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().studentId()).isEqualTo(student.getId());
  }

  @Test
  void student_cannot_access_another_student_transcript() {
    User student = student();
    User otherStudent = student();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(student), otherStudent.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertErrorResponse(response);
  }

  @Test
  void unknown_student_returns_404() {
    User admin = admin();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(admin), UUID.randomUUID(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
  }

  @Test
  void unknown_promotion_returns_404() {
    User admin = admin();
    User student = student();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(admin), student.getId(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
  }

  @Test
  void without_promotion_transcript_contains_all_grades() {
    User student = student();
    Promotion promotionA = promotion();
    Promotion promotionB = promotion();
    grade(student, exam(course(promotionA, "Maths 2023", 6), "2023-11-15T09:00:00Z", 1.0), 12.0);
    grade(student, exam(course(promotionB, "Physique 2024", 5), "2024-11-15T09:00:00Z", 1.0), 14.0);
    User admin = admin();

    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(admin), student.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TranscriptResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.items()).hasSize(2);
    assertThat(body.items())
        .extracting("courseTitle")
        .containsExactly("Maths 2023", "Physique 2024");
    assertThat(body.promotionId()).isNull();
  }

  @Test
  void with_promotion_transcript_is_filtered() {
    User student = student();
    Promotion promotionA = promotion();
    Promotion promotionB = promotion();
    grade(student, exam(course(promotionA, "Maths 2023", 6), "2023-11-15T09:00:00Z", 1.0), 12.0);
    grade(student, exam(course(promotionB, "Physique 2024", 5), "2024-11-15T09:00:00Z", 1.0), 14.0);
    User admin = admin();

    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(admin), student.getId(), promotionA.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TranscriptResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.items()).hasSize(1);
    assertThat(body.items().get(0).courseTitle()).isEqualTo("Maths 2023");
    assertThat(body.promotionId()).isEqualTo(promotionA.getId());
  }

  @Test
  void reuses_persisted_transcript_information() {
    User student = student();
    Transcript persisted = new Transcript();
    persisted.setId(UUID.randomUUID());
    persisted.setStudentId(student.getId());
    persisted.setPromotionId(null);
    persisted.setStatus(TranscriptStatus.GENERATED);
    persisted.setPdfUrl("https://s3.example/transcript.pdf");
    persisted.setEmail("student@school.com");
    persisted.setGeneratedAt(Instant.parse("2024-01-01T10:00:00Z"));
    transcriptRepository.save(persisted);
    User admin = admin();

    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(admin), student.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TranscriptResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.id()).isEqualTo(persisted.getId());
    assertThat(body.status()).isEqualTo(TranscriptStatus.GENERATED);
    assertThat(body.pdfUrl()).isEqualTo("https://s3.example/transcript.pdf");
    assertThat(body.email()).isEqualTo("student@school.com");
    assertThat(body.generatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
  }

  private static void assertErrorResponse(ResponseEntity<ErrorResponse> response) {
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.timestamp()).isNotNull();
    assertThat(body.status()).isEqualTo(response.getStatusCode().value());
    assertThat(body.error()).isNotBlank();
    assertThat(body.message()).isNotBlank();
    assertThat(body.path()).startsWith("/students/");
  }

  private ResponseEntity<TranscriptResponse> getTranscript(
      String token, UUID studentId, UUID promotionId) {
    return exchange(token, studentId, promotionId, TranscriptResponse.class);
  }

  private ResponseEntity<ErrorResponse> getTranscriptError(
      String token, UUID studentId, UUID promotionId) {
    return exchange(token, studentId, promotionId, ErrorResponse.class);
  }

  private <T> ResponseEntity<T> exchange(
      String token, UUID studentId, UUID promotionId, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    String url = "/students/" + studentId + "/transcript";
    if (promotionId != null) {
      url += "?promotionId=" + promotionId;
    }
    return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
  }

  private String token(User user) {
    return tokenProvider.generateToken(user);
  }

  private User student() {
    return user(Role.STUDENT);
  }

  private User admin() {
    return user(Role.ADMIN);
  }

  private User user(Role role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(role);
    return userRepository.save(user);
  }

  private Promotion promotion() {
    Promotion promotion = new Promotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + UUID.randomUUID());
    promotion.setYear(2024);
    return promotionRepository.save(promotion);
  }

  private Course course(Promotion promotion, String title, int credits) {
    Course course = new Course();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotion.getId());
    return courseRepository.save(course);
  }

  private Exam exam(Course course, String date, double coefficient) {
    Exam exam = new Exam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(coefficient);
    return examRepository.save(exam);
  }

  private void grade(User student, Exam exam, double value) {
    Grade grade = new Grade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    gradeRepository.save(grade);
  }
}
