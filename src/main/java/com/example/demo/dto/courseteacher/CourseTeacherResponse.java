package com.example.demo.dto.courseteacher;

import java.util.UUID;

public record CourseTeacherResponse(UUID id, UUID courseId, UUID teacherId) {}
