package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.dto.transcript.TranscriptSendEmailResponse;
import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.Transcript;
import com.example.demo.entity.TranscriptItem;
import com.example.demo.entity.User;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TranscriptMapper;
import com.example.demo.repository.PromotionRepository;
import com.example.demo.repository.TranscriptRepository;
import com.example.demo.repository.UserRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class TranscriptServiceImplTest {

  private TokenProvider tokenProvider;
  private UserRepository userRepository;
  private PromotionRepository promotionRepository;
  private TranscriptRepository transcriptRepository;
  private TranscriptDataBuilder transcriptDataBuilder;
  private EventProducer<TranscriptEmailRequested> eventProducer;
  private TranscriptServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    userRepository = mock(UserRepository.class);
    promotionRepository = mock(PromotionRepository.class);
    transcriptRepository = mock(TranscriptRepository.class);
    transcriptDataBuilder = mock(TranscriptDataBuilder.class);
    eventProducer = mock(EventProducer.class);
    service =
        new TranscriptServiceImpl(
            tokenProvider,
            userRepository,
            promotionRepository,
            transcriptRepository,
            new TranscriptMapper(),
            transcriptDataBuilder,
            eventProducer);
  }

  @Test
  void admin_can_access_any_student_transcript() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptDataBuilder.buildItems(student.getId(), null)).thenReturn(List.of());

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
    when(transcriptDataBuilder.buildItems(student.getId(), null)).thenReturn(List.of());

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
    List<TranscriptItem> items =
        List.of(
            item("Maths", "2023-01-01T09:00:00Z", 1.0, 12.0, 6),
            item("Physique", "2024-01-01T09:00:00Z", 1.0, 14.0, 5));
    when(transcriptDataBuilder.buildItems(student.getId(), null)).thenReturn(items);

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items()).extracting("courseTitle").containsExactly("Maths", "Physique");
    assertThat(response.promotionId()).isNull();
  }

  @Test
  void with_promotion_transcript_is_filtered() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    UUID promotionA = UUID.randomUUID();
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(promotionRepository.existsById(promotionA)).thenReturn(true);
    when(transcriptDataBuilder.buildItems(student.getId(), promotionA))
        .thenReturn(List.of(item("Maths", "2023-01-01T09:00:00Z", 1.0, 12.0, 6)));

    TranscriptResponse response = service.getStudentTranscript(student.getId(), promotionA, jwt);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).courseTitle()).isEqualTo("Maths");
    assertThat(response.promotionId()).isEqualTo(promotionA);
  }

  @Test
  void multiple_grades_produce_multiple_items_in_date_order() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptDataBuilder.buildItems(student.getId(), null))
        .thenReturn(
            List.of(
                item("A-Course", "2023-01-01T09:00:00Z", 1.0, 15.0, 5),
                item("B-Course", "2024-01-01T09:00:00Z", 1.0, 11.0, 6)));

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
    when(transcriptDataBuilder.buildItems(student.getId(), null)).thenReturn(List.of());
    when(transcriptRepository.findByStudentIdAndPromotionIdIsNull(student.getId()))
        .thenReturn(Optional.of(persisted));

    TranscriptResponse response = service.getStudentTranscript(student.getId(), null, jwt);

    assertThat(response.id()).isEqualTo(persisted.getId());
    assertThat(response.status()).isEqualTo(TranscriptStatus.GENERATED);
    assertThat(response.pdfUrl()).isEqualTo("https://s3.example/transcript.pdf");
    assertThat(response.email()).isEqualTo("student@school.com");
    assertThat(response.generatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
  }

  @Test
  void admin_can_request_transcript_email() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptRepository.findByStudentIdAndPromotionIdIsNull(student.getId()))
        .thenReturn(Optional.empty());

    TranscriptSendEmailResponse response = service.requestTranscriptEmail(student.getId(), jwt);

    assertThat(response.status()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(response.message())
        .isEqualTo("Processing in progress, the transcript will be sent by email.");
    ArgumentCaptor<Collection<TranscriptEmailRequested>> captor = eventCaptor();
    verify(eventProducer).accept(captor.capture());
    TranscriptEmailRequested event = captor.getValue().iterator().next();
    assertThat(event.getTranscriptId()).isEqualTo(response.transcriptId());
    verify(transcriptRepository).save(any(Transcript.class));
  }

  @Test
  void student_can_request_own_transcript_email() {
    User student = student();
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(student.getId().toString());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptRepository.findByStudentIdAndPromotionIdIsNull(student.getId()))
        .thenReturn(Optional.empty());

    TranscriptSendEmailResponse response = service.requestTranscriptEmail(student.getId(), jwt);

    assertThat(response.status()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(response.transcriptId()).isNotNull();
  }

  @Test
  void student_cannot_request_another_student_transcript_email() {
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(UUID.randomUUID().toString());

    assertThatThrownBy(() -> service.requestTranscriptEmail(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void unknown_student_request_email_throws_not_found() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestTranscriptEmail(UUID.randomUUID(), jwt))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void request_email_reuses_persisted_transcript_and_resets_to_pending() {
    User student = student();
    Jwt jwt = jwt(Role.ADMIN);
    Transcript persisted = new Transcript();
    persisted.setId(UUID.randomUUID());
    persisted.setStudentId(student.getId());
    persisted.setPromotionId(null);
    persisted.setStatus(TranscriptStatus.EMAIL_SENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptRepository.findByStudentIdAndPromotionIdIsNull(student.getId()))
        .thenReturn(Optional.of(persisted));

    TranscriptSendEmailResponse response = service.requestTranscriptEmail(student.getId(), jwt);

    assertThat(response.transcriptId()).isEqualTo(persisted.getId());
    assertThat(persisted.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(persisted.getEmail()).isEqualTo(student.getEmail());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ArgumentCaptor<Collection<TranscriptEmailRequested>> eventCaptor() {
    return ArgumentCaptor.forClass(Collection.class);
  }

  private static TranscriptItem item(
      String courseTitle, String examDate, double coefficient, double grade, int credits) {
    TranscriptItem item = new TranscriptItem();
    item.setId(UUID.randomUUID());
    item.setTranscriptId(UUID.randomUUID());
    item.setCourseTitle(courseTitle);
    item.setExamDate(Instant.parse(examDate));
    item.setCoefficient(coefficient);
    item.setGrade(grade);
    item.setCredits(credits);
    return item;
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
}
