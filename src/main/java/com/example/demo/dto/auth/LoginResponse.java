package com.example.demo.dto.auth;

import com.example.demo.enums.Role;
import java.util.UUID;

public record LoginResponse(
    String token, UUID userId, String firstName, String lastName, String email, Role role) {}
