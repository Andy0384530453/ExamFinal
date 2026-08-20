package com.example.demo.mapper;

import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourseTeacher;
import org.springframework.stereotype.Component;

@Component
public class CourseTeacherMapper {

  public CourseTeacherResponse toResponse(JCourseTeacher assignment) {
    return new CourseTeacherResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getTeacherId());
  }
}
