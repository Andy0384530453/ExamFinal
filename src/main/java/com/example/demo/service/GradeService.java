package com.example.demo.service;

import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface GradeService {

  List<GradeResponse> listGradesForCourse(UUID courseId, Jwt jwt);

  GradeResponse updateGrade(UUID gradeId, GradeUpdateRequest request, Jwt jwt);

  List<GradeHistoryResponse> getGradeHistory(UUID gradeId, Jwt jwt);
}