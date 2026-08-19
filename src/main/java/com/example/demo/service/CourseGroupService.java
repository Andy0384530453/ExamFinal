package com.example.demo.service;

import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import java.util.List;
import java.util.UUID;

public interface CourseGroupService {

  List<CourseGroupResponse> listGroups(UUID courseId);

  CourseGroupResponse assign(UUID courseId, CourseGroupAssignRequest request);

  void remove(UUID courseId, UUID groupId);
}
