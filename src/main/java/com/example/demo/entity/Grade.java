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
public class Grade {
  @Id private String id;

  @Column(nullable = false)
  private String studentId;

  @Column(nullable = false)
  private String examId;

  @Column(nullable = false)
  private Double value;

  private String comment;

  @Column(nullable = false)
  private Instant modifiedAt;

  @Column(nullable = false)
  private String modifiedBy;
}
