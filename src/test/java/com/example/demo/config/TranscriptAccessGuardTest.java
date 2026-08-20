package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.enums.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class TranscriptAccessGuardTest {

  private TokenProvider tokenProvider;
  private TranscriptAccessGuard guard;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    guard = new TranscriptAccessGuard(new AccessGuard(tokenProvider));
  }

  @Test
  void admin_can_access_any_student() {
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.ADMIN.name());

    assertThatCode(() -> guard.checkStudentAccess(UUID.randomUUID(), jwt))
        .doesNotThrowAnyException();
  }

  @Test
  void student_can_access_own_transcript() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(studentId.toString());

    assertThatCode(() -> guard.checkStudentAccess(studentId, jwt)).doesNotThrowAnyException();
  }

  @Test
  void student_cannot_access_another_student_transcript() {
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.STUDENT.name());
    when(tokenProvider.getUserId(jwt)).thenReturn(UUID.randomUUID().toString());

    assertThatThrownBy(() -> guard.checkStudentAccess(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void teacher_cannot_access_transcript() {
    Jwt jwt = jwt(Role.TEACHER);
    when(tokenProvider.getRole(jwt)).thenReturn(Role.TEACHER.name());

    assertThatThrownBy(() -> guard.checkStudentAccess(UUID.randomUUID(), jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static Jwt jwt(Role role) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .claim("role", role.name())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .build();
  }
}
