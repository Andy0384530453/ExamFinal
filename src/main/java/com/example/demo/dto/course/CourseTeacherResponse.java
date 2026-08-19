package com.example.demo.dto.course;

import java.util.UUID;

public record CourseTeacherResponse(UUID id, UUID courseId, UUID teacherId) {}
