package com.example.demo.entity;

import com.example.demo.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
public class JUser {
  @Id private UUID id;

  @Column(nullable = false)
  private String ref;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;
}
