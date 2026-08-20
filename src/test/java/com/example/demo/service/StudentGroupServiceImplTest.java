package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.AccessGuard;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import com.example.demo.validator.EntityValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class StudentGroupServiceImplTest {

  private TokenProvider tokenProvider;
  private JUserRepository userRepository;
  private JGroupRepository groupRepository;
  private JStudentGroupRepository studentGroupRepository;
  private StudentGroupServiceImpl service;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    userRepository = mock(JUserRepository.class);
    groupRepository = mock(JGroupRepository.class);
    studentGroupRepository = mock(JStudentGroupRepository.class);
    EntityValidator validator =
        new EntityValidator(null, groupRepository, userRepository, null, null, null, null);
    service =
        new StudentGroupServiceImpl(
            new AccessGuard(tokenProvider), studentGroupRepository, validator);
  }

  @Test
  void get_history_returns_mapped_memberships() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn("ADMIN");
    JStudentGroup membership = membership(studentId, UUID.randomUUID());
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(studentGroupRepository.findByStudentIdOrderByStartDateDesc(studentId))
        .thenReturn(List.of(membership));

    List<StudentGroupResponse> history = service.getStudentGroupHistory(studentId, jwt);

    assertThat(history).hasSize(1);
    StudentGroupResponse response = history.get(0);
    assertThat(response.id()).isEqualTo(membership.getId());
    assertThat(response.studentId()).isEqualTo(studentId);
    assertThat(response.groupId()).isEqualTo(membership.getGroupId());
    assertThat(response.startDate()).isEqualTo(membership.getStartDate());
    assertThat(response.endDate()).isEqualTo(membership.getEndDate());
  }

  @Test
  void get_history_student_can_access_own() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn("STUDENT");
    when(tokenProvider.getUserId(jwt)).thenReturn(studentId.toString());
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(studentGroupRepository.findByStudentIdOrderByStartDateDesc(studentId))
        .thenReturn(List.of());

    List<StudentGroupResponse> history = service.getStudentGroupHistory(studentId, jwt);

    assertThat(history).isEmpty();
  }

  @Test
  void get_history_another_student_is_denied() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.STUDENT);
    when(tokenProvider.getRole(jwt)).thenReturn("STUDENT");
    when(tokenProvider.getUserId(jwt)).thenReturn(UUID.randomUUID().toString());

    assertThatThrownBy(() -> service.getStudentGroupHistory(studentId, jwt))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void get_history_unknown_student_throws_not_found() {
    UUID studentId = UUID.randomUUID();
    Jwt jwt = jwt(Role.ADMIN);
    when(tokenProvider.getRole(jwt)).thenReturn("ADMIN");
    when(userRepository.existsById(studentId)).thenReturn(false);

    assertThatThrownBy(() -> service.getStudentGroupHistory(studentId, jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Student not found");
  }

  @Test
  void change_group_closes_current_and_creates_new() {
    UUID studentId = UUID.randomUUID();
    UUID oldGroupId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    JStudentGroup current = membership(studentId, oldGroupId);
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(group(newGroupId)));
    when(studentGroupRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.of(current));

    StudentGroupResponse response =
        service.changeStudentGroup(studentId, new StudentGroupChangeRequest(newGroupId, null));

    assertThat(response.groupId()).isEqualTo(newGroupId);
    assertThat(response.studentId()).isEqualTo(studentId);
    assertThat(response.endDate()).isNull();
    assertThat(current.getEndDate()).isNotNull();

    ArgumentCaptor<JStudentGroup> captor = ArgumentCaptor.forClass(JStudentGroup.class);
    verify(studentGroupRepository, org.mockito.Mockito.times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).hasSize(2);
    assertThat(captor.getAllValues().get(1).getGroupId()).isEqualTo(newGroupId);
  }

  @Test
  void change_group_without_current_membership_only_creates() {
    UUID studentId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(group(newGroupId)));
    when(studentGroupRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.empty());

    StudentGroupResponse response =
        service.changeStudentGroup(studentId, new StudentGroupChangeRequest(newGroupId, null));

    assertThat(response.groupId()).isEqualTo(newGroupId);
    verify(studentGroupRepository).save(org.mockito.ArgumentMatchers.any(JStudentGroup.class));
  }

  @Test
  void change_group_with_custom_start_date_uses_it() {
    UUID studentId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    LocalDate startDate = LocalDate.of(2025, 9, 1);
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(group(newGroupId)));
    when(studentGroupRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.empty());

    StudentGroupResponse response =
        service.changeStudentGroup(studentId, new StudentGroupChangeRequest(newGroupId, startDate));

    assertThat(response.startDate()).isEqualTo(startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
  }

  @Test
  void change_group_unknown_student_throws_not_found() {
    UUID studentId = UUID.randomUUID();
    when(userRepository.existsById(studentId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.changeStudentGroup(
                    studentId, new StudentGroupChangeRequest(UUID.randomUUID(), null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Student not found");
    verify(studentGroupRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void change_group_unknown_group_throws_not_found() {
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(userRepository.existsById(studentId)).thenReturn(true);
    when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.changeStudentGroup(studentId, new StudentGroupChangeRequest(groupId, null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Group not found");
    verify(studentGroupRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private static JGroup group(UUID groupId) {
    JGroup group = new JGroup();
    group.setId(groupId);
    group.setRef("GRP-" + UUID.randomUUID());
    return group;
  }

  private static JStudentGroup membership(UUID studentId, UUID groupId) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(studentId);
    membership.setGroupId(groupId);
    membership.setStartDate(Instant.parse("2024-09-01T08:00:00Z"));
    membership.setEndDate(null);
    return membership;
  }

  private static Jwt jwt(Role role) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(UUID.randomUUID().toString())
        .claim("role", role.name())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .build();
  }
}
