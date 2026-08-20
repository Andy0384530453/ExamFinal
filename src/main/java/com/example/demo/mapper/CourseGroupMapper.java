package com.example.demo.mapper;

import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.entity.JCourseGroup;
import org.springframework.stereotype.Component;

@Component
public class CourseGroupMapper {

  public CourseGroupResponse toResponse(JCourseGroup assignment) {
    return new CourseGroupResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getGroupId());
  }
}
