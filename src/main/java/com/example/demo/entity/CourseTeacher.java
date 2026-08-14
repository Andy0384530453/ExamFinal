package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CourseTeacher {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID courseId;

  @Column(nullable = false)
  private UUID teacherId;
}
