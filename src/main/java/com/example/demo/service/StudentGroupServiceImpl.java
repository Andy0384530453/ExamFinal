package com.example.demo.service;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.enums.Role;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.validator.EntityValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {

  private final TokenProvider tokenProvider;
  private final JStudentGroupRepository studentGroupRepository;
  private final EntityValidator validator;

  public StudentGroupServiceImpl(
      TokenProvider tokenProvider,
      JStudentGroupRepository studentGroupRepository,
      EntityValidator validator) {
    this.tokenProvider = tokenProvider;
    this.studentGroupRepository = studentGroupRepository;
    this.validator = validator;
  }

  @Override
  public List<StudentGroupResponse> getStudentGroupHistory(UUID studentId, Jwt jwt) {
    checkAdminOrStudentSelf(studentId, jwt);
    validator.assertStudentExists(studentId);
    return studentGroupRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public StudentGroupResponse changeStudentGroup(
      UUID studentId, StudentGroupChangeRequest request) {
    validator.assertStudentExists(studentId);
    validator.requireGroup(request.groupId());
    Instant startDate =
        request.startDate() == null
            ? today()
            : request.startDate().atStartOfDay(ZoneOffset.UTC).toInstant();

    Optional<JStudentGroup> current =
        studentGroupRepository.findByStudentIdAndEndDateIsNull(studentId);
    if (current.isPresent()) {
      JStudentGroup previous = current.get();
      previous.setEndDate(today());
      studentGroupRepository.save(previous);
    }

    JStudentGroup assignment = new JStudentGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setStudentId(studentId);
    assignment.setGroupId(request.groupId());
    assignment.setStartDate(startDate);
    studentGroupRepository.save(assignment);

    return toResponse(assignment);
  }

  private void checkAdminOrStudentSelf(UUID studentId, Jwt jwt) {
    String role = tokenProvider.getRole(jwt);
    if (Role.ADMIN.name().equals(role)) {
      return;
    }
    if (Role.STUDENT.name().equals(role)) {
      UUID authenticatedId = UUID.fromString(tokenProvider.getUserId(jwt));
      if (!authenticatedId.equals(studentId)) {
        throw new AccessDeniedException("A student can only access their own group history");
      }
      return;
    }
    throw new AccessDeniedException("Access denied: insufficient role");
  }

  private Instant today() {
    return LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private StudentGroupResponse toResponse(JStudentGroup membership) {
    return new StudentGroupResponse(
        membership.getId(),
        membership.getStudentId(),
        membership.getGroupId(),
        membership.getStartDate(),
        membership.getEndDate());
  }
}
