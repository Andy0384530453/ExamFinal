package com.example.demo.service;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.dto.course.CourseGroupResponse;
import com.example.demo.dto.course.CourseTeacherResponse;
import com.example.demo.entity.JCourseGroup;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JUserRepository;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseAssignmentServiceImpl implements CourseAssignmentService {

  private final GradeAccessGuard accessGuard;
  private final JCourseRepository courseRepository;
  private final JGroupRepository groupRepository;
  private final JUserRepository userRepository;
  private final JCourseTeacherRepository courseTeacherRepository;
  private final JCourseGroupRepository courseGroupRepository;

  public CourseAssignmentServiceImpl(
      GradeAccessGuard accessGuard,
      JCourseRepository courseRepository,
      JGroupRepository groupRepository,
      JUserRepository userRepository,
      JCourseTeacherRepository courseTeacherRepository,
      JCourseGroupRepository courseGroupRepository) {
    this.accessGuard = accessGuard;
    this.courseRepository = courseRepository;
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.courseTeacherRepository = courseTeacherRepository;
    this.courseGroupRepository = courseGroupRepository;
  }

  @Override
  @Transactional
  public CourseTeacherResponse assignTeacher(UUID courseId, UUID teacherId) {
    requireCourse(courseId);
    requireTeacher(teacherId);
    if (courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId)) {
      throw new ConflictException(
          "Teacher " + teacherId + " is already assigned to course " + courseId);
    }
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setTeacherId(teacherId);
    courseTeacherRepository.save(assignment);
    return toTeacherResponse(assignment);
  }

  @Override
  @Transactional
  public void removeTeacher(UUID courseId, UUID teacherId) {
    JCourseTeacher assignment = requireTeacherAssignment(courseId, teacherId);
    courseTeacherRepository.delete(assignment);
  }

  @Override
  @Transactional
  public CourseGroupResponse assignGroup(UUID courseId, UUID groupId, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    requireCourse(courseId);
    requireGroup(groupId);
    if (courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId).isPresent()) {
      throw new ConflictException(
          "Group " + groupId + " is already associated with course " + courseId);
    }
    JCourseGroup assignment = new JCourseGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setGroupId(groupId);
    courseGroupRepository.save(assignment);
    return toGroupResponse(assignment);
  }

  @Override
  @Transactional
  public void removeGroup(UUID courseId, UUID groupId, Jwt jwt) {
    accessGuard.checkAdminOrTeacherOfCourse(courseId, jwt);
    JCourseGroup assignment =
        courseGroupRepository
            .findByCourseIdAndGroupId(courseId, groupId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Group " + groupId + " is not associated with course " + courseId));
    courseGroupRepository.delete(assignment);
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

  private void requireTeacher(UUID teacherId) {
    JUser teacher =
        userRepository
            .findById(teacherId)
            .filter(user -> user.getRole() == Role.TEACHER)
            .orElseThrow(
                () -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
  }

  private JCourseTeacher requireTeacherAssignment(UUID courseId, UUID teacherId) {
    return courseTeacherRepository
        .findByCourseIdAndTeacherId(courseId, teacherId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Teacher " + teacherId + " is not assigned to course " + courseId));
  }

  private CourseTeacherResponse toTeacherResponse(JCourseTeacher assignment) {
    return new CourseTeacherResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getTeacherId());
  }

  private CourseGroupResponse toGroupResponse(JCourseGroup assignment) {
    return new CourseGroupResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getGroupId());
  }
}
