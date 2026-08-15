package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "course_group")
@Getter
@Setter
public class JCourseGroup {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID courseId;

  @Column(nullable = false)
  private UUID groupId;
}
