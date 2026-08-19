package com.example.demo.dto.coursegroup;

import java.util.UUID;

public record CourseGroupResponse(UUID id, UUID courseId, UUID groupId) {}
