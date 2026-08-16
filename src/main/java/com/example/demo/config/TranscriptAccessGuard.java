package com.example.demo.config;

import com.example.demo.enums.Role;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class TranscriptAccessGuard {

  private final TokenProvider tokenProvider;

  public TranscriptAccessGuard(TokenProvider tokenProvider) {
    this.tokenProvider = tokenProvider;
  }

  public void checkStudentAccess(UUID studentId, Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (Role.ADMIN.name().equals(role)) {
      return;
    }
    if (Role.STUDENT.name().equals(role)) {
      UUID authenticatedId = UUID.fromString(tokenProvider.getUserId(jwt));
      if (!authenticatedId.equals(studentId)) {
        throw new AccessDeniedException("A student can only access their own transcript");
      }
      return;
    }
    throw new AccessDeniedException("Access denied: insufficient role");
  }
}
