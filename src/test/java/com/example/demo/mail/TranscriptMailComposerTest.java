package com.example.demo.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptMailComposerTest {

  private final TranscriptMailComposer composer = new TranscriptMailComposer();

  @Test
  void builds_email_to_student_with_download_link() throws Exception {
    JUser student = student();
    String pdfUrl = "https://s3.example/transcripts/transcript.pdf";

    Email email = composer.buildTranscriptEmail(student, pdfUrl);

    assertThat(email.to().getAddress()).isEqualTo(student.getEmail());
    assertThat(email.subject()).isEqualTo("Your grade transcript");
    assertThat(email.htmlBody()).contains(pdfUrl);
    assertThat(email.htmlBody()).contains("Download your transcript");
    assertThat(email.cc()).isEmpty();
    assertThat(email.bcc()).isEmpty();
    assertThat(email.attachments()).isEmpty();
  }

  private static JUser student() {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF");
    user.setFirstName("Lucas");
    user.setLastName("Moreau");
    user.setEmail("student@school.com");
    user.setRole(Role.STUDENT);
    return user;
  }
}
