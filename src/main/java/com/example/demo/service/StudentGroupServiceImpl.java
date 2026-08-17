package com.example.demo.service;

import com.example.demo.config.TokenProvider;
import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
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
  private final JUserRepository userRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupRepository studentGroupRepository;

  public StudentGroupServiceImpl(
      TokenProvider tokenProvider,
      JUserRepository userRepository,
      JGroupRepository groupRepository,
      JStudentGroupRepository studentGroupRepository) {
    this.tokenProvider = tokenProvider;
    this.userRepository = userRepository;
    this.groupRepository = groupRepository;
    this.studentGroupRepository = studentGroupRepository;
  }

  @Override
  public List<StudentGroupResponse> getStudentGroupHistory(UUID studentId, Jwt jwt) {
    checkAdminOrStudentSelf(studentId, jwt);
    requireStudent(studentId);
    return studentGroupRepository.findByStudentIdOrderByStartDateDesc(studentId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public StudentGroupResponse changeStudentGroup(
      UUID studentId, StudentGroupChangeRequest request) {
    requireStudent(studentId);
    requireGroup(request.groupId());
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

  private void requireStudent(UUID studentId) {
    if (!userRepository.existsById(studentId)) {
      throw new ResourceNotFoundException("Student not found with id: " + studentId);
    }
  }

  private void requireGroup(UUID groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw new ResourceNotFoundException("Group not found with id: " + groupId);
    }
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
