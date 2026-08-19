package com.example.demo.dto.coursegroup;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CourseGroupAssignRequest(@NotNull UUID groupId) {}
