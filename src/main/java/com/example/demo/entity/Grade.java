package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Grade {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID studentId;

  @Column(nullable = false)
  private UUID examId;

  @Column(nullable = false)
  private Double value;

  private String comment;

  @Column(nullable = false)
  private Instant modifiedAt;

  @Column(nullable = false)
  private UUID modifiedBy;
}
