package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenProviderTest {

  private static final String SECRET = "test-secret-key-for-unit-tests-123456";
  private static final long EXPIRATION_HOURS = 1;

  private TokenProvider tokenProvider;

  @BeforeEach
  void setUp() {
    tokenProvider = new TokenProvider(SECRET, EXPIRATION_HOURS);
  }

  @Test
  void generateToken_returns_non_empty_string() {
    JUser user = createUser(Role.STUDENT);

    String token = tokenProvider.generateToken(user);

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  void validateToken_returns_valid_jwt() {
    JUser user = createUser(Role.STUDENT);

    String token = tokenProvider.generateToken(user);
    Jwt jwt = tokenProvider.validateToken(token);

    assertThat(jwt).isNotNull();
    assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
    assertThat(jwt.getClaimAsString("role")).isEqualTo(Role.STUDENT.name());
  }

  @Test
  void getUserId_returns_user_id_from_token() {
    UUID userId = UUID.randomUUID();
    JUser user = createUser(Role.STUDENT, userId);

    String token = tokenProvider.generateToken(user);
    Jwt jwt = tokenProvider.validateToken(token);
    String returnedId = tokenProvider.getUserId(jwt);

    assertThat(returnedId).isEqualTo(userId.toString());
  }

  @Test
  void getRole_returns_role_from_token() {
    JUser user = createUser(Role.TEACHER);

    String token = tokenProvider.generateToken(user);
    Jwt jwt = tokenProvider.validateToken(token);
    String role = tokenProvider.getRole(jwt);

    assertThat(role).isEqualTo(Role.TEACHER.name());
  }

  @Test
  void getRole_returns_admin_role_for_admin_user() {
    JUser user = createUser(Role.ADMIN);

    String token = tokenProvider.generateToken(user);
    Jwt jwt = tokenProvider.validateToken(token);
    String role = tokenProvider.getRole(jwt);

    assertThat(role).isEqualTo(Role.ADMIN.name());
  }

  @Test
  void jwtDecoder_returns_decoder() {
    JwtDecoder decoder = tokenProvider.jwtDecoder();

    assertThat(decoder).isNotNull();
  }

  @Test
  void validateToken_with_invalid_token_throws_exception() {
    assertThatThrownBy(() -> tokenProvider.validateToken("invalid.token.here"))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void generateToken_different_users_produce_different_tokens() {
    JUser user1 = createUser(Role.STUDENT, UUID.randomUUID());
    JUser user2 = createUser(Role.TEACHER, UUID.randomUUID());

    String token1 = tokenProvider.generateToken(user1);
    String token2 = tokenProvider.generateToken(user2);

    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  void validateToken_with_wrong_secret_throws_exception() {
    TokenProvider wrongProvider = new TokenProvider("wrong-secret-key-123456", EXPIRATION_HOURS);
    JUser user = createUser(Role.STUDENT);

    String token = tokenProvider.generateToken(user);

    assertThatThrownBy(() -> wrongProvider.validateToken(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void generateToken_with_different_roles_produces_correct_claims() {
    for (Role role : Role.values()) {
      JUser user = createUser(role);
      String token = tokenProvider.generateToken(user);
      Jwt jwt = tokenProvider.validateToken(token);

      assertThat(jwt.getClaimAsString("role")).isEqualTo(role.name());
    }
  }

  private static JUser createUser(Role role) {
    return createUser(role, UUID.randomUUID());
  }

  private static JUser createUser(Role role, UUID id) {
    JUser user = new JUser();
    user.setId(id);
    user.setRef("REF-001");
    user.setFirstName("Alice");
    user.setLastName("Dupont");
    user.setEmail("alice@test.com");
    user.setPassword("hashed");
    user.setRole(role);
    return user;
  }
}
