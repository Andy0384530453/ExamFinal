package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transcript_item")
@Getter
@Setter
public class JTranscriptItem {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID transcriptId;

  @Column(nullable = false)
  private String courseTitle;

  @Column(nullable = false)
  private Instant examDate;

  @Column(nullable = false)
  private Double coefficient;

  private Double grade;

  @Column(nullable = false)
  private int credits;
}
