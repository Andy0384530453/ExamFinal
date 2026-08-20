package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.repository.JUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private JUserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private TokenProvider tokenProvider;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    userRepository = mock(JUserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    tokenProvider = mock(TokenProvider.class);
    authService = new AuthService(userRepository, passwordEncoder, tokenProvider);
  }

  @Test
  void login_with_valid_credentials_returns_token() {
    UUID userId = UUID.randomUUID();
    JUser user = new JUser();
    user.setId(userId);
    user.setFirstName("Alice");
    user.setLastName("Dupont");
    user.setEmail("alice@test.com");
    user.setPassword("hashed-password");
    user.setRole(Role.STUDENT);

    when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
    when(tokenProvider.generateToken(user)).thenReturn("jwt-token-abc");

    LoginResponse response = authService.login(new LoginRequest("alice@test.com", "password123"));

    assertThat(response.token()).isEqualTo("jwt-token-abc");
    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.firstName()).isEqualTo("Alice");
    assertThat(response.lastName()).isEqualTo("Dupont");
    assertThat(response.email()).isEqualTo("alice@test.com");
    assertThat(response.role()).isEqualTo(Role.STUDENT);
  }

  @Test
  void login_with_wrong_password_throws_illegal_argument() {
    JUser user = new JUser();
    user.setPassword("hashed-password");

    when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("alice@test.com", "wrong-password")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email or password");
  }

  @Test
  void login_with_unknown_email_throws_illegal_argument() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@test.com", "password123")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email or password");
  }

  @Test
  void login_with_teacher_role_returns_teacher_role() {
    UUID userId = UUID.randomUUID();
    JUser user = new JUser();
    user.setId(userId);
    user.setFirstName("Bob");
    user.setLastName("Martin");
    user.setEmail("bob@test.com");
    user.setPassword("hashed-password");
    user.setRole(Role.TEACHER);

    when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
    when(tokenProvider.generateToken(user)).thenReturn("jwt-token-teach");

    LoginResponse response = authService.login(new LoginRequest("bob@test.com", "password123"));

    assertThat(response.role()).isEqualTo(Role.TEACHER);
    assertThat(response.token()).isEqualTo("jwt-token-teach");
  }

  @Test
  void login_with_admin_role_returns_admin_role() {
    UUID userId = UUID.randomUUID();
    JUser user = new JUser();
    user.setId(userId);
    user.setFirstName("Claire");
    user.setLastName("Durand");
    user.setEmail("claire@test.com");
    user.setPassword("hashed-password");
    user.setRole(Role.ADMIN);

    when(userRepository.findByEmail("claire@test.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
    when(tokenProvider.generateToken(user)).thenReturn("jwt-token-admin");

    LoginResponse response = authService.login(new LoginRequest("claire@test.com", "password123"));

    assertThat(response.role()).isEqualTo(Role.ADMIN);
    assertThat(response.token()).isEqualTo("jwt-token-admin");
  }
}
