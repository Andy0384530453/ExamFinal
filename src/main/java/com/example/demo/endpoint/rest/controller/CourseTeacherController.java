package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.service.CourseTeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseTeacherController {

  private final CourseTeacherService courseTeacherService;

  public CourseTeacherController(CourseTeacherService courseTeacherService) {
    this.courseTeacherService = courseTeacherService;
  }

  @GetMapping("/courses/{courseId}/teachers")
  public List<CourseTeacherResponse> listTeachers(@PathVariable UUID courseId) {
    return courseTeacherService.listTeachers(courseId);
  }

  @PostMapping("/courses/{courseId}/teachers")
  public ResponseEntity<CourseTeacherResponse> assign(
      @PathVariable UUID courseId, @RequestBody @Valid CourseTeacherAssignRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseTeacherService.assign(courseId, request));
  }

  @DeleteMapping("/courses/{courseId}/teachers/{teacherId}")
  public ResponseEntity<Void> remove(@PathVariable UUID courseId, @PathVariable UUID teacherId) {
    courseTeacherService.remove(courseId, teacherId);
    return ResponseEntity.noContent().build();
  }
}
