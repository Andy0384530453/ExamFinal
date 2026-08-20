package com.example.demo.service;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.entity.JUser;
import com.example.demo.repository.JUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final JUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenProvider tokenProvider;

  public AuthService(
      JUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenProvider tokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
  }

  public LoginResponse login(LoginRequest request) {
    JUser user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid email or password");
    }

    String token = tokenProvider.generateToken(user);

    return new LoginResponse(
        token,
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole());
  }
}
