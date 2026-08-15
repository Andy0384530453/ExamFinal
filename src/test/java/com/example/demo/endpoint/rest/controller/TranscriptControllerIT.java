package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptResponse;
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
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;
  @Autowired private JTranscriptRepository transcriptRepository;

  @Test
  void admin_can_access_any_student_transcript() {
    JUser student = student();
    JPromotion promotion = promotion();
    JCourse course = course(promotion, "Mathematiques", 6);
    JExam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);
    JUser admin = admin();

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
    JUser student = student();
    ResponseEntity<TranscriptResponse> response =
        getTranscript(token(student), student.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().studentId()).isEqualTo(student.getId());
  }

  @Test
  void student_cannot_access_another_student_transcript() {
    JUser student = student();
    JUser otherStudent = student();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(student), otherStudent.getId(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertErrorResponse(response);
  }

  @Test
  void unknown_student_returns_404() {
    JUser admin = admin();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(admin), UUID.randomUUID(), null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
  }

  @Test
  void unknown_promotion_returns_404() {
    JUser admin = admin();
    JUser student = student();

    ResponseEntity<ErrorResponse> response =
        getTranscriptError(token(admin), student.getId(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertErrorResponse(response);
  }

  @Test
  void without_promotion_transcript_contains_all_grades() {
    JUser student = student();
    JPromotion promotionA = promotion();
    JPromotion promotionB = promotion();
    grade(student, exam(course(promotionA, "Maths 2023", 6), "2023-11-15T09:00:00Z", 1.0), 12.0);
    grade(student, exam(course(promotionB, "Physique 2024", 5), "2024-11-15T09:00:00Z", 1.0), 14.0);
    JUser admin = admin();

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
    JUser student = student();
    JPromotion promotionA = promotion();
    JPromotion promotionB = promotion();
    grade(student, exam(course(promotionA, "Maths 2023", 6), "2023-11-15T09:00:00Z", 1.0), 12.0);
    grade(student, exam(course(promotionB, "Physique 2024", 5), "2024-11-15T09:00:00Z", 1.0), 14.0);
    JUser admin = admin();

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
    JUser student = student();
    JTranscript persisted = new JTranscript();
    persisted.setId(UUID.randomUUID());
    persisted.setStudentId(student.getId());
    persisted.setPromotionId(null);
    persisted.setStatus(TranscriptStatus.GENERATED);
    persisted.setPdfUrl("https://s3.example/transcript.pdf");
    persisted.setEmail("student@school.com");
    persisted.setGeneratedAt(Instant.parse("2024-01-01T10:00:00Z"));
    transcriptRepository.save(persisted);
    JUser admin = admin();

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
