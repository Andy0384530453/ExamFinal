package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.entity.JUser;
import com.example.demo.enums.TranscriptStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.pdf.TranscriptPdfGenerator;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.service.TranscriptDataBuilder;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptEmailRequestedService implements Consumer<TranscriptEmailRequested> {

  private static final String BUCKET_KEY_PREFIX = "transcripts/";
  private static final String PDF_SUFFIX = ".pdf";

  private final JTranscriptRepository transcriptRepository;
  private final JUserRepository userRepository;
  private final TranscriptDataBuilder transcriptDataBuilder;
  private final TranscriptPdfGenerator pdfGenerator;
  private final BucketComponent bucketComponent;
  private final Mailer mailer;

  @Override
  public void accept(TranscriptEmailRequested event) {
    JTranscript transcript = findTranscript(event.getTranscriptId());
    try {
      JUser student = findStudent(transcript.getStudentId());
      List<JTranscriptItem> items =
          transcriptDataBuilder.buildItems(transcript.getStudentId(), null);
      File pdf = pdfGenerator.generate(transcript.getId(), student, items);
      String bucketKey = BUCKET_KEY_PREFIX + transcript.getId() + PDF_SUFFIX;
      bucketComponent.upload(pdf, bucketKey);
      String pdfUrl = bucketComponent.presign(bucketKey, Duration.ofDays(7)).toString();

      transcript.setPdfUrl(pdfUrl);
      transcript.setGeneratedAt(Instant.now());
      transcript.setStatus(TranscriptStatus.GENERATED);
      transcriptRepository.save(transcript);

      mailer.accept(buildEmail(student, pdfUrl));
      transcript.setStatus(TranscriptStatus.EMAIL_SENT);
      transcriptRepository.save(transcript);
      log.info(
          "Transcript {} generated, uploaded to S3 and sent by email to {}",
          transcript.getId(),
          student.getEmail());
    } catch (Exception e) {
      log.error("Failed to generate and send transcript {}", transcript.getId(), e);
      transcript.setStatus(TranscriptStatus.FAILED);
      transcriptRepository.save(transcript);
    }
  }

  private JTranscript findTranscript(UUID transcriptId) {
    return transcriptRepository
        .findById(transcriptId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Transcript not found with id: " + transcriptId));
  }

  private JUser findStudent(UUID studentId) {
    return userRepository
        .findById(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student not found with id: " + studentId));
  }

  private Email buildEmail(JUser student, String pdfUrl) throws Exception {
    return new Email(
        new InternetAddress(student.getEmail()),
        List.of(),
        List.of(),
        "Your grade transcript",
        "<p>Hi,</p><p>Here is your grade transcript:</p><p><a href=\""
            + pdfUrl
            + "\">Download your transcript</a></p>",
        List.of());
  }
}
