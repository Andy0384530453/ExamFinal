package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Group {
  @Id private String id;

  @Column(nullable = false, unique = true)
  private String ref;

  @Column(nullable = false)
  private String promotionId;
}
