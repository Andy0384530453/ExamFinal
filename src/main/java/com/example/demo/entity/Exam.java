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
public class Exam {
  @Id private String id;

  @Column(nullable = false)
  private String courseId;

  @Column(nullable = false, unique = true)
  private String ref;

  @Column(nullable = false)
  private Instant dateExam;

  @Column(nullable = false)
  private Double coefficient;
}
