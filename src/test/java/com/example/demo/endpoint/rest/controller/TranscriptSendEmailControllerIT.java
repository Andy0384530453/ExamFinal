package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptSendEmailResponse;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class TranscriptSendEmailControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private TranscriptRepository transcriptRepository;

  @MockBean private EventProducer<TranscriptEmailRequested> eventProducer;

  @Test
  void admin_can_send_transcript_email_and_gets_202() {
    User student = student();
    Promotion promotion = promotion();
    Course course = course(promotion, "Mathematiques", 6);
    Exam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);
    User admin = admin();

    ResponseEntity<TranscriptSendEmailResponse> response = sendEmail(token(admin), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    TranscriptSendEmailResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.transcriptId()).isNotNull();
    assertThat(body.status()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(body.message())
        .isEqualTo("Processing in progress, the transcript will be sent by email.");
    assertTranscriptPending(body.transcriptId(), student.getEmail());
    verify(eventProducer).accept(any());
  }

  @Test
  void student_can_send_own_transcript_email_and_gets_202() {
    User student = student();

    ResponseEntity<TranscriptSendEmailResponse> response =
        sendEmail(token(student), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(TranscriptStatus.PENDING);
  }

  @Test
  void student_cannot_send_another_student_transcript_email() {
    User student = student();
    User otherStudent = student();

    ResponseEntity<ErrorResponse> response = sendEmailError(token(student), otherStudent.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertErrorResponse(response);
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_student_returns_404() {
    User admin = admin();

    ResponseEntity<ErrorResponse> response = sendEmailError(token(admin), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void transcript_created_with_pending_status_and_processing_is_async() {
    User student = student();
    User admin = admin();

    ResponseEntity<TranscriptSendEmailResponse> response = sendEmail(token(admin), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    UUID transcriptId = response.getBody().transcriptId();

    Transcript transcript = transcriptRepository.findById(transcriptId).orElseThrow();
    assertThat(transcript.getStudentId()).isEqualTo(student.getId());
    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(transcript.getEmail()).isEqualTo(student.getEmail());
    assertThat(transcript.getPdfUrl()).isNull();
    assertThat(transcript.getGeneratedAt()).isNull();
  }

  private void assertTranscriptPending(UUID transcriptId, String email) {
    Transcript transcript = transcriptRepository.findById(transcriptId).orElseThrow();
    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(transcript.getEmail()).isEqualTo(email);
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

  private ResponseEntity<TranscriptSendEmailResponse> sendEmail(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/transcript/send-email",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        TranscriptSendEmailResponse.class);
  }

  private ResponseEntity<ErrorResponse> sendEmailError(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/transcript/send-email",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        ErrorResponse.class);
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
