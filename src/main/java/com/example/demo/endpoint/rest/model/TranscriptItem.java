package com.example.demo.endpoint.rest.model;

import java.time.Instant;

public record TranscriptItem(
    String courseTitle, Instant examDate, double coefficient, Double grade, Integer credits) {}
