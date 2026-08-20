package com.example.demo.config;

import com.example.demo.PojaGenerated;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.repository.JUserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@PojaGenerated
@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final JUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(JUserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    seedUser(
        UUID.fromString("a0000000-0000-0000-0000-000000000001"),
        "STU-001",
        "Alice",
        "Dupont",
        "student@test.com",
        "password123",
        Role.STUDENT);
    seedUser(
        UUID.fromString("a0000000-0000-0000-0000-000000000002"),
        "TCH-001",
        "Bob",
        "Martin",
        "teacher@test.com",
        "password123",
        Role.TEACHER);
    seedUser(
        UUID.fromString("a0000000-0000-0000-0000-000000000003"),
        "ADM-001",
        "Claire",
        "Durand",
        "admin@test.com",
        "password123",
        Role.ADMIN);
  }

  private void seedUser(
      UUID id,
      String ref,
      String firstName,
      String lastName,
      String email,
      String password,
      Role role) {
    if (userRepository.findByEmail(email).isPresent()) {
      return;
    }
    JUser user = new JUser();
    user.setId(id);
    user.setRef(ref);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    userRepository.save(user);
    log.info("Seeded {} user: {}", role, email);
  }
}
