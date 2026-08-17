package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import com.example.demo.service.StudentGroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentGroupController {

  private final StudentGroupService studentGroupService;

  public StudentGroupController(StudentGroupService studentGroupService) {
    this.studentGroupService = studentGroupService;
  }

  @GetMapping("/students/{id}/groups")
  public List<StudentGroupResponse> getStudentGroupHistory(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return studentGroupService.getStudentGroupHistory(id, jwt);
  }

  @PostMapping("/students/{id}/groups")
  public ResponseEntity<StudentGroupResponse> changeStudentGroup(
      @PathVariable("id") UUID id, @RequestBody @Valid StudentGroupChangeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(studentGroupService.changeStudentGroup(id, request));
  }
}
