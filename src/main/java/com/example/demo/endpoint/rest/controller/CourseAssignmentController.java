package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.course.CourseGroupAssignmentRequest;
import com.example.demo.dto.course.CourseGroupResponse;
import com.example.demo.dto.course.CourseTeacherResponse;
import com.example.demo.dto.course.TeacherAssignmentRequest;
import com.example.demo.service.CourseAssignmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseAssignmentController {

  private final CourseAssignmentService courseAssignmentService;

  public CourseAssignmentController(CourseAssignmentService courseAssignmentService) {
    this.courseAssignmentService = courseAssignmentService;
  }

  @PostMapping("/courses/{id}/teachers")
  public ResponseEntity<CourseTeacherResponse> assignTeacher(
      @PathVariable("id") UUID id, @RequestBody @Valid TeacherAssignmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseAssignmentService.assignTeacher(id, request.teacherId()));
  }

  @DeleteMapping("/courses/{id}/teachers/{teacherId}")
  public ResponseEntity<Void> removeTeacher(
      @PathVariable("id") UUID id, @PathVariable("teacherId") UUID teacherId) {
    courseAssignmentService.removeTeacher(id, teacherId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/courses/{id}/groups")
  public ResponseEntity<CourseGroupResponse> assignGroup(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @RequestBody @Valid CourseGroupAssignmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseAssignmentService.assignGroup(id, request.groupId(), jwt));
  }

  @DeleteMapping("/courses/{id}/groups/{groupId}")
  public ResponseEntity<Void> removeGroup(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @PathVariable("groupId") UUID groupId) {
    courseAssignmentService.removeGroup(id, groupId, jwt);
    return ResponseEntity.noContent().build();
  }
}
