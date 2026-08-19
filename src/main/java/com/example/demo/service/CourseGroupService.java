package com.example.demo.service;

import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface CourseGroupService {

  List<CourseGroupResponse> listGroups(UUID courseId);

  CourseGroupResponse assign(UUID courseId, CourseGroupAssignRequest request, Jwt jwt);

  void remove(UUID courseId, UUID groupId, Jwt jwt);
}
