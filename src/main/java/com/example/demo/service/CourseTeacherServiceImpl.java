package com.example.demo.service;

import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

  private final JCourseTeacherRepository courseTeacherRepository;
  private final JCourseRepository courseRepository;
  private final JUserRepository userRepository;

  public CourseTeacherServiceImpl(
      JCourseTeacherRepository courseTeacherRepository,
      JCourseRepository courseRepository,
      JUserRepository userRepository) {
    this.courseTeacherRepository = courseTeacherRepository;
    this.courseRepository = courseRepository;
    this.userRepository = userRepository;
  }

  @Override
  public List<CourseTeacherResponse> listTeachers(UUID courseId) {
    requireCourse(courseId);
    return courseTeacherRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public CourseTeacherResponse assign(UUID courseId, CourseTeacherAssignRequest request) {
    requireCourse(courseId);
    requireUser(request.teacherId());
    if (courseTeacherRepository.existsByTeacherIdAndCourseId(request.teacherId(), courseId)) {
      throw new ConflictException(
          "Teacher " + request.teacherId() + " is already assigned to course " + courseId);
    }
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setTeacherId(request.teacherId());
    courseTeacherRepository.save(assignment);
    return toResponse(assignment);
  }

  @Override
  @Transactional
  public void remove(UUID courseId, UUID teacherId) {
    requireCourse(courseId);
    JCourseTeacher assignment =
        courseTeacherRepository
            .findByCourseIdAndTeacherId(courseId, teacherId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Teacher " + teacherId + " is not assigned to course " + courseId));
    courseTeacherRepository.delete(assignment);
  }

  private CourseTeacherResponse toResponse(JCourseTeacher assignment) {
    return new CourseTeacherResponse(
        assignment.getId(), assignment.getCourseId(), assignment.getTeacherId());
  }

  private void requireCourse(UUID courseId) {
    if (!courseRepository.existsById(courseId)) {
      throw new ResourceNotFoundException("Course not found with id: " + courseId);
    }
  }

  private void requireUser(UUID userId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("Teacher not found with id: " + userId);
    }
  }
}
