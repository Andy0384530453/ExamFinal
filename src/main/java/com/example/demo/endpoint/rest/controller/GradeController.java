package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import com.example.demo.service.GradeService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @PutMapping("/grades/{id}")
  public GradeResponse updateGrade(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") UUID id,
      @RequestBody GradeUpdateRequest request) {
    return gradeService.updateGrade(id, request, jwt);
  }

  @GetMapping("/grades/{id}/history")
  public List<GradeHistoryResponse> getGradeHistory(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return gradeService.getGradeHistory(id, jwt);
  }
}