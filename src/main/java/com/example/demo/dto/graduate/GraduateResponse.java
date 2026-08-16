package com.example.demo.dto.graduate;

import java.util.UUID;

public record GraduateResponse(
    UUID studentId, String firstName, String lastName, String email, double average) {}
