package com.example.demo.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.enums.TranscriptStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JTranscriptTest {

  private final JTranscript transcript = transcript();

  @Test
  void new_transcript_is_pending() {
    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
  }

  @Test
  void mark_pending_sets_status_and_email() {
    transcript.markPending("student@school.com");

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(transcript.getEmail()).isEqualTo("student@school.com");
  }

  @Test
  void mark_pending_resets_generated_transcript() {
    transcript.markPending("student@school.com");
    transcript.markGenerated("https://s3.example/transcript.pdf", Instant.now());

    transcript.markPending("other@school.com");

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.PENDING);
    assertThat(transcript.getEmail()).isEqualTo("other@school.com");
  }

  @Test
  void mark_generated_sets_pdf_url_and_timestamp() {
    Instant generatedAt = Instant.parse("2024-01-01T10:00:00Z");
    transcript.markPending("student@school.com");

    transcript.markGenerated("https://s3.example/transcript.pdf", generatedAt);

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.GENERATED);
    assertThat(transcript.getPdfUrl()).isEqualTo("https://s3.example/transcript.pdf");
    assertThat(transcript.getGeneratedAt()).isEqualTo(generatedAt);
  }

  @Test
  void mark_email_sent_requires_generated() {
    transcript.markPending("student@school.com");
    transcript.markGenerated("https://s3.example/transcript.pdf", Instant.now());

    transcript.markEmailSent();

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.EMAIL_SENT);
  }

  @Test
  void cannot_mark_email_sent_from_pending() {
    assertThatThrownBy(transcript::markEmailSent).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cannot_mark_generated_from_email_sent() {
    transcript.markPending("student@school.com");
    transcript.markGenerated("https://s3.example/transcript.pdf", Instant.now());
    transcript.markEmailSent();

    assertThatThrownBy(
            () -> transcript.markGenerated("https://s3.example/other.pdf", Instant.now()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void mark_failed_is_allowed_from_pending() {
    transcript.markFailed();

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.FAILED);
  }

  @Test
  void mark_failed_is_allowed_from_generated() {
    transcript.markPending("student@school.com");
    transcript.markGenerated("https://s3.example/transcript.pdf", Instant.now());

    transcript.markFailed();

    assertThat(transcript.getStatus()).isEqualTo(TranscriptStatus.FAILED);
  }

  private static JTranscript transcript() {
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(UUID.randomUUID());
    return transcript;
  }
}
