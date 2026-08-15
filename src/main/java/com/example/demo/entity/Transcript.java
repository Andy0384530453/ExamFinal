package com.example.demo.entity;

import com.example.demo.enums.TranscriptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Transcript {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID studentId;

  private UUID promotionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TranscriptStatus status = TranscriptStatus.PENDING;

  private String pdfUrl;

  private String email;

  private Instant generatedAt;
}
