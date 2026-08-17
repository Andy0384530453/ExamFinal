package com.example.demo.service;

import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface StudentGroupService {

  List<StudentGroupResponse> getStudentGroupHistory(UUID studentId, Jwt jwt);

  StudentGroupResponse changeStudentGroup(UUID studentId, StudentGroupChangeRequest request);
}
