package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.entity.JUser;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.mail.Mailer;
import com.example.demo.mail.TranscriptMailComposer;
import com.example.demo.pdf.TranscriptPdfGenerator;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.service.TranscriptDataBuilder;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TranscriptEmailRequestedService implements Consumer<TranscriptEmailRequested> {

  private static final String BUCKET_KEY_PREFIX = "transcripts/";
  private static final String PDF_SUFFIX = ".pdf";

  private final JTranscriptRepository transcriptRepository;
  private final JUserRepository userRepository;
  private final TranscriptDataBuilder transcriptDataBuilder;
  private final TranscriptPdfGenerator pdfGenerator;
  private final BucketComponent bucketComponent;
  private final TranscriptMailComposer mailComposer;
  private final Mailer mailer;

  public TranscriptEmailRequestedService(
      JTranscriptRepository transcriptRepository,
      JUserRepository userRepository,
      TranscriptDataBuilder transcriptDataBuilder,
      TranscriptPdfGenerator pdfGenerator,
      BucketComponent bucketComponent,
      TranscriptMailComposer mailComposer,
      Mailer mailer) {
    this.transcriptRepository = transcriptRepository;
    this.userRepository = userRepository;
    this.transcriptDataBuilder = transcriptDataBuilder;
    this.pdfGenerator = pdfGenerator;
    this.bucketComponent = bucketComponent;
    this.mailComposer = mailComposer;
    this.mailer = mailer;
  }

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

      transcript.markGenerated(pdfUrl, Instant.now());
      transcriptRepository.save(transcript);

      mailer.accept(mailComposer.buildTranscriptEmail(student, pdfUrl));
      transcript.markEmailSent();
      transcriptRepository.save(transcript);
      log.info(
          "Transcript {} generated, uploaded to S3 and sent by email to {}",
          transcript.getId(),
          student.getEmail());
    } catch (Exception e) {
      log.error("Failed to generate and send transcript {}", transcript.getId(), e);
      transcript.markFailed();
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
}
