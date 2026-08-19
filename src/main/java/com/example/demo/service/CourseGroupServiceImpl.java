package com.example.demo.service;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.entity.JCourseGroup;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JGroupRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseGroupServiceImpl implements CourseGroupService {

  private final GradeAccessGuard accessGuard;
  private final JCourseGroupRepository courseGroupRepository;
  private final JCourseRepository courseRepository;
  private final JGroupRepository groupRepository;

  public CourseGroupServiceImpl(
      GradeAccessGuard accessGuard,
      JCourseGroupRepository courseGroupRepository,
      JCourseRepository courseRepository,
      JGroupRepository groupRepository) {
    this.accessGuard = accessGuard;
    this.courseGroupRepository = courseGroupRepository;
    this.courseRepository = courseRepository;
    this.groupRepository = groupRepository;
  }

  @Override
  public List<CourseGroupResponse> listGroups(UUID courseId) {
    requireCourse(courseId);
    return courseGroupRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public CourseGroupResponse assign(UUID courseId, CourseGroupAssignRequest request, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    requireCourse(courseId);
    requireGroup(request.groupId());
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

  private void requireCourse(UUID courseId) {
    if (!courseRepository.existsById(courseId)) {
      throw new ResourceNotFoundException("Course not found with id: " + courseId);
    }
  }

  private void requireGroup(UUID groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw new ResourceNotFoundException("Group not found with id: " + groupId);
    }
  }
}
