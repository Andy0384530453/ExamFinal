package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.grade.GradeCreateRequest;
import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import com.example.demo.service.GradeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GradeController {

  private final GradeService gradeService;

  public GradeController(GradeService gradeService) {
    this.gradeService = gradeService;
  }

  @GetMapping("/courses/{courseId}/grades")
  public List<GradeResponse> listGradesForCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("courseId") UUID courseId) {
    return gradeService.listGradesForCourse(courseId, jwt);
  }

  @PostMapping("/courses/{courseId}/grades")
  public ResponseEntity<GradeResponse> createGrade(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("courseId") UUID courseId,
      @RequestBody @Valid GradeCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(gradeService.createGrade(courseId, request, jwt));
  }

  @GetMapping("/students/{id}/grades")
  public List<GradeResponse> listGradesForStudent(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return gradeService.listGradesForStudent(id, jwt);
  }

  @PutMapping("/grades/{id}")
  public GradeResponse updateGrade(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @RequestBody @Valid GradeUpdateRequest request) {
    return gradeService.updateGrade(id, request, jwt);
  }

  @GetMapping("/grades/{id}/history")
  public List<GradeHistoryResponse> getGradeHistory(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return gradeService.getGradeHistory(id, jwt);
  }
}
