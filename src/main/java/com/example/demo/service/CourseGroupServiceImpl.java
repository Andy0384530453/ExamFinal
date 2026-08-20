package com.example.demo.service;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.entity.JCourseGroup;
import com.example.demo.exception.ConflictException;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.validator.EntityValidator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseGroupServiceImpl implements CourseGroupService {

  private final GradeAccessGuard accessGuard;
  private final JCourseGroupRepository courseGroupRepository;
  private final EntityValidator validator;

  public CourseGroupServiceImpl(
      GradeAccessGuard accessGuard,
      JCourseGroupRepository courseGroupRepository,
      EntityValidator validator) {
    this.accessGuard = accessGuard;
    this.courseGroupRepository = courseGroupRepository;
    this.validator = validator;
  }

  @Override
  public List<CourseGroupResponse> listGroups(UUID courseId) {
    validator.requireCourse(courseId);
    return courseGroupRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public CourseGroupResponse assign(UUID courseId, CourseGroupAssignRequest request, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    validator.requireCourse(courseId);
    validator.requireGroup(request.groupId());
    if (courseGroupRepository.findByCourseIdAndGroupId(courseId, request.groupId()).isPresent()) {
      throw new ConflictException(
          "Group " + request.groupId() + " is already associated with course " + courseId);
    }
    JCourseGroup assignment = new JCourseGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setGroupId(request.groupId());
    courseGroupRepository.save(assignment);
    return toResponse(assignment);
  }

  @Override
  @Transactional
  public void remove(UUID courseId, UUID groupId, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    courseGroupRepository
        .findByCourseIdAndGroupId(courseId, groupId)
        .ifPresent(courseGroupRepository::delete);
  }

  private CourseGroupResponse toResponse(JCourseGroup assignment) {
    return new CourseGroupResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getGroupId());
  }
}
