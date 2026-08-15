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
@Table(name = "exam")
@Getter
@Setter
public class JExam {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID courseId;

  @Column(nullable = false, unique = true)
  private String ref;

  @Column(nullable = false)
  private Instant dateExam;

  @Column(nullable = false)
  private Double coefficient;
}
