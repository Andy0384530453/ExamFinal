package com.example.demo.service;

import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import java.util.List;
import java.util.UUID;

public interface CourseTeacherService {

  List<CourseTeacherResponse> listTeachers(UUID courseId);

  CourseTeacherResponse assign(UUID courseId, CourseTeacherAssignRequest request);

  void remove(UUID courseId, UUID teacherId);
}
