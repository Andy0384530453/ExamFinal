package com.example.demo.config;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TranscriptAccessGuard {

  private final AccessGuard accessGuard;

  public TranscriptAccessGuard(AccessGuard accessGuard) {
    this.accessGuard = accessGuard;
  }

  public void checkStudentAccess(UUID studentId, Jwt jwt) {
    accessGuard.checkAdminOrStudentSelf(
        studentId, jwt, "A student can only access their own transcript");
  }
}
