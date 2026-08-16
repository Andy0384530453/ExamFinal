package com.example.demo.conf;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.file.bucket.BucketConf;
import com.example.demo.mail.EmailConf;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.service.event.TranscriptEmailRequestedService;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.VerifyEmailIdentityRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TranscriptEmailLocalstackIT {

  private static final PostgresConf POSTGRES_CONF = new PostgresConf();
  private static final LocalstackConf LOCALSTACK_CONF = new LocalstackConf();

  static {
    System.setProperty("aws.accessKeyId", "test");
    System.setProperty("aws.secretAccessKey", "test");
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    POSTGRES_CONF.configureProperties(registry);
    LOCALSTACK_CONF.configureProperties(registry);
    registry.add("app.security.jwt-secret", () -> "test-jwt-secret-for-localstack-it");
  }

  @BeforeAll
  static void beforeAll() {
    POSTGRES_CONF.start();
    LOCALSTACK_CONF.start();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  POSTGRES_CONF.stop();
                  LOCALSTACK_CONF.stop();
                }));
  }

  @Autowired private TranscriptEmailRequestedService transcriptEmailRequestedService;
  @Autowired private JTranscriptRepository transcriptRepository;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;
  @Autowired private SesClient sesClient;
  @Autowired private BucketConf bucketConf;
  @Autowired private EmailConf emailConf;

  @Test
  void generates_pdf_uploads_to_s3_and_sends_email_with_download_link() throws Exception {
    sesClient.verifyEmailIdentity(
        VerifyEmailIdentityRequest.builder().emailAddress(emailConf.getSesSource()).build());
    bucketConf
        .getS3Client()
        .createBucket(CreateBucketRequest.builder().bucket(bucketConf.getBucketName()).build());

    JUser student = student();
    JPromotion promotion = promotion();
    JCourse course = course(promotion, "Mathematiques", 6);
    JExam exam = exam(course, "2024-01-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);
    JTranscript transcript = transcript(student);

    transcriptEmailRequestedService.accept(new TranscriptEmailRequested(transcript.getId()));

    JTranscript saved = transcriptRepository.findById(transcript.getId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(TranscriptStatus.EMAIL_SENT);
    assertThat(saved.getPdfUrl()).isNotBlank();
    assertThat(saved.getGeneratedAt()).isNotNull();

    assertPresignedUrlIsAValidPdf(saved.getPdfUrl());

    String sentEmails = fetchLocalstackSentEmails();
    assertThat(sentEmails).contains(student.getEmail());
    assertThat(sentEmails).contains("Your grade transcript");
    assertThat(sentEmails).contains(saved.getPdfUrl());
  }

  private void assertPresignedUrlIsAValidPdf(String presignedUrl) throws Exception {
    try (InputStream in = URI.create(presignedUrl).toURL().openStream()) {
      byte[] bytes = in.readAllBytes();
      assertThat(bytes.length).isGreaterThan(100);
      assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
  }

  private String fetchLocalstackSentEmails() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(LOCALSTACK_CONF.getEndpoint() + "/_aws/ses"))
            .GET()
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return response.body();
  }

  private JUser student() {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("Lucas");
    user.setLastName("Moreau");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(Role.STUDENT);
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

  private JTranscript transcript(JUser student) {
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(student.getId());
    transcript.setStatus(TranscriptStatus.PENDING);
    return transcriptRepository.save(transcript);
  }
}
