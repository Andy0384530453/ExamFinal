package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class GradeModification {
  @Id private String id;

  @Column(nullable = false)
  private String gradeId;

  @Column(nullable = false)
  private Double oldValue;

  @Column(nullable = false)
  private Double newValue;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Instant modifiedAt;

  @Column(nullable = false)
  private String modifiedBy;
}
