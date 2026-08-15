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
@Table(name = "grade_modification")
@Getter
@Setter
public class JGradeModification {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID gradeId;

  @Column(nullable = false)
  private Double oldValue;

  @Column(nullable = false)
  private Double newValue;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Instant modifiedAt;

  @Column(nullable = false)
  private UUID modifiedBy;
}
