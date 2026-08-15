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
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
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
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;
  @Autowired private JTranscriptRepository transcriptRepository;

  @MockBean private EventProducer<TranscriptEmailRequested> eventProducer;

  @Test
  void admin_can_send_transcript_email_and_gets_202() {
    JUser student = student();
    JPromotion promotion = promotion();
    JCourse course = course(promotion, "Mathematiques", 6);
    JExam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);
    JUser admin = admin();

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
    JUser student = student();

    ResponseEntity<TranscriptSendEmailResponse> response =
        sendEmail(token(student), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(TranscriptStatus.PENDING);
  }

  @Test
  void student_cannot_send_another_student_transcript_email() {
    JUser student = student();
    JUser otherStudent = student();

    ResponseEntity<ErrorResponse> response = sendEmailError(token(student), otherStudent.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertErrorResponse(response);
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void unknown_student_returns_404() {
    JUser admin = admin();

    ResponseEntity<ErrorResponse> response = sendEmailError(token(admin), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void transcript_created_with_pending_status_and_processing_is_async() {
    JUser student = student();
    JUser admin = admin();

    ResponseEntity<TranscriptSendEmailResponse> response = sendEmail(token(admin), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    UUID transcriptId = response.getBody().transcriptId();

    JTranscript transcript = transcriptRepository.findById(transcriptId).orElseThrow();
    assertThat(transcript.getStudentId()).isEqualTo(student.getId());
    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(transcript.getEmail()).isEqualTo(student.getEmail());
    assertThat(transcript.getPdfUrl()).isNull();
    assertThat(transcript.getGeneratedAt()).isNull();
  }

  private void assertTranscriptPending(UUID transcriptId, String email) {
    JTranscript transcript = transcriptRepository.findById(transcriptId).orElseThrow();
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

  private String token(JUser user) {
    return tokenProvider.generateToken(user);
  }

  private JUser student() {
    return user(Role.STUDENT);
  }

  private JUser admin() {
    return user(Role.ADMIN);
  }

  private JUser user(Role role) {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(role);
    return userRepository.save(user);
  }

  private JPromotion promotion() {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + UUID.randomUUID());
    promotion.setYear(2024);
    return promotionRepository.save(promotion);
  }

  private JCourse course(JPromotion promotion, String title, int credits) {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotion.getId());
    return courseRepository.save(course);
  }

  private JExam exam(JCourse course, String date, double coefficient) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(coefficient);
    return examRepository.save(exam);
  }

  private void grade(JUser student, JExam exam, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    gradeRepository.save(grade);
  }
}
