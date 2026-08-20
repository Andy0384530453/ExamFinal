package com.example.demo.service;

import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.validator.EntityValidator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

  private final JCourseTeacherRepository courseTeacherRepository;
  private final EntityValidator validator;

  public CourseTeacherServiceImpl(
      JCourseTeacherRepository courseTeacherRepository, EntityValidator validator) {
    this.courseTeacherRepository = courseTeacherRepository;
    this.validator = validator;
  }

  @Override
  public List<CourseTeacherResponse> listTeachers(UUID courseId) {
    validator.requireCourse(courseId);
    return courseTeacherRepository.findByCourseId(courseId).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public CourseTeacherResponse assign(UUID courseId, CourseTeacherAssignRequest request) {
    validator.requireCourse(courseId);
    validator.requireTeacher(request.teacherId());
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
    validator.requireCourse(courseId);
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
}
