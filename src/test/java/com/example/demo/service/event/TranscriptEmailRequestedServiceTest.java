package com.example.demo.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.pdf.TranscriptPdfGenerator;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.service.TranscriptDataBuilder;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TranscriptEmailRequestedServiceTest {

  private JTranscriptRepository transcriptRepository;
  private JUserRepository userRepository;
  private TranscriptDataBuilder transcriptDataBuilder;
  private TranscriptPdfGenerator pdfGenerator;
  private BucketComponent bucketComponent;
  private Mailer mailer;
  private TranscriptEmailRequestedService service;

  @BeforeEach
  void setUp() {
    transcriptRepository = mock(JTranscriptRepository.class);
    userRepository = mock(JUserRepository.class);
    transcriptDataBuilder = mock(TranscriptDataBuilder.class);
    pdfGenerator = mock(TranscriptPdfGenerator.class);
    bucketComponent = mock(BucketComponent.class);
    mailer = mock(Mailer.class);
    service =
        new TranscriptEmailRequestedService(
            transcriptRepository,
            userRepository,
            transcriptDataBuilder,
            pdfGenerator,
            bucketComponent,
            mailer);
  }

  @Test
  void successful_processing_sets_email_sent_with_pdf_url_and_generated_at() throws Exception {
    JUser student = student();
    JTranscript transcript = transcript(student);
    when(transcriptRepository.findById(transcript.getId())).thenReturn(Optional.of(transcript));
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(transcriptDataBuilder.buildItems(student.getId(), null))
        .thenReturn(List.of(item("Maths", 14.5)));
    when(pdfGenerator.generate(any(UUID.class), any(JUser.class), any()))
        .thenReturn(new File("/tmp/transcript.pdf"));
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://s3.example/transcripts/transcript.pdf"));

    service.accept(new TranscriptEmailRequested(transcript.getId()));

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.EMAIL_SENT);
    assertThat(transcript.getPdfUrl()).isEqualTo("https://s3.example/transcripts/transcript.pdf");
    assertThat(transcript.getGeneratedAt()).isNotNull();
    verify(bucketComponent).upload(any(File.class), anyString());
    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    Email email = emailCaptor.getValue();
    assertThat(email.to().getAddress()).isEqualTo(student.getEmail());
    assertThat(email.htmlBody()).contains("https://s3.example/transcripts/transcript.pdf");
  }

  @Test
  void failed_processing_sets_transcript_status_to_failed() {
    JUser student = student();
    JTranscript transcript = transcript(student);
    when(transcriptRepository.findById(transcript.getId())).thenReturn(Optional.of(transcript));
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
    when(pdfGenerator.generate(any(UUID.class), any(JUser.class), any()))
        .thenThrow(new RuntimeException("PDF generation failed"));

    service.accept(new TranscriptEmailRequested(transcript.getId()));

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.FAILED);
    verify(transcriptRepository).save(transcript);
    verify(mailer, never()).accept(any());
  }

  private static JTranscript transcript(JUser student) {
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(student.getId());
    transcript.setStatus(TranscriptStatus.PENDING);
    return transcript;
  }

  private static JTranscriptItem item(String courseTitle, double grade) {
    JTranscriptItem item = new JTranscriptItem();
    item.setId(UUID.randomUUID());
    item.setTranscriptId(UUID.randomUUID());
    item.setCourseTitle(courseTitle);
    item.setExamDate(Instant.parse("2024-01-01T09:00:00Z"));
    item.setCoefficient(1.5);
    item.setGrade(grade);
    item.setCredits(6);
    return item;
  }

  private static JUser student() {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("Lucas");
    user.setLastName("Moreau");
    user.setEmail("student@school.com");
    user.setRole(Role.STUDENT);
    return user;
  }
}
