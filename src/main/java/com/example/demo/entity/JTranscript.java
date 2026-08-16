package com.example.demo.entity;

import com.example.demo.enums.TranscriptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transcript")
@Getter
public class JTranscript {
  @Setter @Id private UUID id;

  @Setter
  @Column(nullable = false)
  private UUID studentId;

  @Setter private UUID promotionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TranscriptStatus status = TranscriptStatus.PENDING;

  @Column(length = 2048)
  private String pdfUrl;

  private String email;

  private Instant generatedAt;

  public void markPending(String email) {
    this.status = TranscriptStatus.PENDING;
    this.email = email;
  }

  public void markGenerated(String pdfUrl, Instant generatedAt) {
    if (status != TranscriptStatus.PENDING && status != TranscriptStatus.GENERATED) {
      throw new IllegalStateException("Cannot mark as GENERATED from " + status);
    }
    this.status = TranscriptStatus.GENERATED;
    this.pdfUrl = pdfUrl;
    this.generatedAt = generatedAt;
  }

  public void markEmailSent() {
    if (status != TranscriptStatus.GENERATED) {
      throw new IllegalStateException("Cannot mark as EMAIL_SENT from " + status);
    }
    this.status = TranscriptStatus.EMAIL_SENT;
  }

  public void markFailed() {
    if (status != TranscriptStatus.FAILED) {
      this.status = TranscriptStatus.FAILED;
    }
  }
}
