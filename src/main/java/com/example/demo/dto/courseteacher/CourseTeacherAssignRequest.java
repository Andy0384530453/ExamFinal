package com.example.demo.dto.courseteacher;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseTeacherAssignRequest(@NotNull UUID teacherId) {}
