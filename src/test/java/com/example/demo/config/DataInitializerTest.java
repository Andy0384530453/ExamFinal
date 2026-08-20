package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.repository.JUserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataInitializerTest {

  private JUserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private DataInitializer dataInitializer;

  @BeforeEach
  void setUp() {
    userRepository = mock(JUserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    dataInitializer = new DataInitializer(userRepository, passwordEncoder);
  }

  @Test
  void run_seeds_all_three_users_when_none_exist() {
    when(userRepository.findByEmail(org.mockito.ArgumentMatchers.any(String.class)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

    dataInitializer.run();

    ArgumentCaptor<JUser> captor = ArgumentCaptor.forClass(JUser.class);
    verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());

    List<JUser> savedUsers = captor.getAllValues();
    assertThat(savedUsers).hasSize(3);

    JUser student =
        savedUsers.stream().filter(u -> u.getRole() == Role.STUDENT).findFirst().orElseThrow();
    assertThat(student.getRef()).isEqualTo("STU-001");
    assertThat(student.getFirstName()).isEqualTo("Alice");
    assertThat(student.getLastName()).isEqualTo("Dupont");
    assertThat(student.getEmail()).isEqualTo("hei.andy.100@gmail.com");
    assertThat(student.getPassword()).isEqualTo("encoded-password");

    JUser teacher =
        savedUsers.stream().filter(u -> u.getRole() == Role.TEACHER).findFirst().orElseThrow();
    assertThat(teacher.getRef()).isEqualTo("TCH-001");
    assertThat(teacher.getFirstName()).isEqualTo("Bob");
    assertThat(teacher.getLastName()).isEqualTo("Martin");
    assertThat(teacher.getEmail()).isEqualTo("teacher@test.com");

    JUser admin =
        savedUsers.stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow();
    assertThat(admin.getRef()).isEqualTo("ADM-001");
    assertThat(admin.getFirstName()).isEqualTo("Claire");
    assertThat(admin.getLastName()).isEqualTo("Durand");
    assertThat(admin.getEmail()).isEqualTo("admin@test.com");
  }

  @Test
  void run_does_not_save_if_all_users_already_exist() {
    when(userRepository.findByEmail("hei.andy.100@gmail.com")).thenReturn(Optional.of(new JUser()));
    when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(new JUser()));
    when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(new JUser()));

    dataInitializer.run();

    verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(JUser.class));
  }

  @Test
  void run_seeds_only_missing_users() {
    when(userRepository.findByEmail("hei.andy.100@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(new JUser()));
    when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(new JUser()));
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

    dataInitializer.run();

    ArgumentCaptor<JUser> captor = ArgumentCaptor.forClass(JUser.class);
    verify(userRepository).save(captor.capture());

    assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
  }

  @Test
  void run_encodes_passwords() {
    when(userRepository.findByEmail(org.mockito.ArgumentMatchers.any(String.class)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("password123")).thenReturn("encoded");

    dataInitializer.run();

    verify(passwordEncoder, org.mockito.Mockito.times(3)).encode("password123");
  }

  @Test
  void run_sets_correct_uuids_for_seeded_users() {
    when(userRepository.findByEmail(org.mockito.ArgumentMatchers.any(String.class)))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("password123")).thenReturn("encoded");

    dataInitializer.run();

    ArgumentCaptor<JUser> captor = ArgumentCaptor.forClass(JUser.class);
    verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());

    List<JUser> savedUsers = captor.getAllValues();
    assertThat(savedUsers).allSatisfy(user -> assertThat(user.getId()).isNotNull());
  }
}
