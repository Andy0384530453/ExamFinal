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
@Table(name = "student_group")
@Getter
@Setter
public class JStudentGroup {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID studentId;

  @Column(nullable = false)
  private UUID groupId;

  @Column(nullable = false)
  private Instant startDate;

  private Instant endDate;
}
