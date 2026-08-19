package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.service.CourseGroupService;
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
public class CourseGroupController {

  private final CourseGroupService courseGroupService;

  public CourseGroupController(CourseGroupService courseGroupService) {
    this.courseGroupService = courseGroupService;
  }

  @GetMapping("/courses/{courseId}/groups")
  public List<CourseGroupResponse> listGroups(@PathVariable UUID courseId) {
    return courseGroupService.listGroups(courseId);
  }

  @PostMapping("/courses/{courseId}/groups")
  public ResponseEntity<CourseGroupResponse> assign(
      @PathVariable UUID courseId, @RequestBody @Valid CourseGroupAssignRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseGroupService.assign(courseId, request));
  }

  @DeleteMapping("/courses/{courseId}/groups/{groupId}")
  public ResponseEntity<Void> remove(@PathVariable UUID courseId, @PathVariable UUID groupId) {
    courseGroupService.remove(courseId, groupId);
    return ResponseEntity.noContent().build();
  }
}
