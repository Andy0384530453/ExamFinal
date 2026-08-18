package com.example.demo.dto.course;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseGroupAssignmentRequest(@NotNull UUID groupId) {}
