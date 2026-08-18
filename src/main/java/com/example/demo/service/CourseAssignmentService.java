package com.example.demo.service;

import com.example.demo.dto.course.CourseGroupResponse;
import com.example.demo.dto.course.CourseTeacherResponse;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CourseAssignmentService {

  CourseTeacherResponse assignTeacher(UUID courseId, UUID teacherId);

  void removeTeacher(UUID courseId, UUID teacherId);

  CourseGroupResponse assignGroup(UUID courseId, UUID groupId, Jwt jwt);

  void removeGroup(UUID courseId, UUID groupId, Jwt jwt);
}
