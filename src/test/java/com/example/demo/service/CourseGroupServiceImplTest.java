package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JCourseGroup;
import com.example.demo.entity.JGroup;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.CourseGroupMapper;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.validator.EntityValidator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

class CourseGroupServiceImplTest {

  private GradeAccessGuard accessGuard;
  private JCourseGroupRepository courseGroupRepository;
  private JCourseRepository courseRepository;
  private JGroupRepository groupRepository;
  private CourseGroupServiceImpl service;

  @BeforeEach
  void setUp() {
    accessGuard = mock(GradeAccessGuard.class);
    courseGroupRepository = mock(JCourseGroupRepository.class);
    courseRepository = mock(JCourseRepository.class);
    groupRepository = mock(JGroupRepository.class);
    EntityValidator validator =
        new EntityValidator(courseRepository, groupRepository, null, null, null, null, null);
    service =
        new CourseGroupServiceImpl(
            accessGuard, courseGroupRepository, validator, new CourseGroupMapper());
  }

  @Test
  void list_groups_returns_mapped_responses() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
    JCourseGroup assignment = new JCourseGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setGroupId(UUID.randomUUID());
    when(courseGroupRepository.findByCourseId(courseId)).thenReturn(List.of(assignment));

    List<CourseGroupResponse> result = service.listGroups(courseId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).courseId()).isEqualTo(courseId);
    assertThat(result.get(0).groupId()).isEqualTo(assignment.getGroupId());
  }

  @Test
  void list_groups_unknown_course_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.listGroups(courseId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_saves_and_returns_response() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId)));
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.empty());

    CourseGroupResponse response =
        service.assign(courseId, new CourseGroupAssignRequest(groupId), jwt);

    assertThat(response.courseId()).isEqualTo(courseId);
    assertThat(response.groupId()).isEqualTo(groupId);
    assertThat(response.id()).isNotNull();
    verify(accessGuard).checkAdminOrTeacherOfCourse(courseId, jwt);
    ArgumentCaptor<JCourseGroup> captor = ArgumentCaptor.forClass(JCourseGroup.class);
    verify(courseGroupRepository).save(captor.capture());
    assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
    assertThat(captor.getValue().getGroupId()).isEqualTo(groupId);
  }

  @Test
  void assign_already_assigned_throws_conflict() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JCourseGroup existing = new JCourseGroup();
    existing.setId(UUID.randomUUID());
    existing.setCourseId(courseId);
    existing.setGroupId(groupId);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group(groupId)));
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.assign(courseId, new CourseGroupAssignRequest(groupId), jwt))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already associated");
    verify(courseGroupRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void assign_unknown_course_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.assign(courseId, new CourseGroupAssignRequest(UUID.randomUUID()), jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_unknown_group_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId)));
    when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assign(courseId, new CourseGroupAssignRequest(groupId), jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Group not found");
  }

  @Test
  void remove_deletes_assignment_if_present() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JCourseGroup assignment = new JCourseGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setGroupId(groupId);
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.of(assignment));

    service.remove(courseId, groupId, jwt);

    verify(accessGuard).checkAdminOrTeacherOfCourse(courseId, jwt);
    verify(courseGroupRepository).delete(assignment);
  }

  @Test
  void remove_noop_when_not_present() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.empty());

    service.remove(courseId, groupId, jwt);

    verify(courseGroupRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  private static JCourse course(UUID courseId) {
    JCourse course = new JCourse();
    course.setId(courseId);
    course.setRef("C-" + UUID.randomUUID());
    return course;
  }

  private static JGroup group(UUID groupId) {
    JGroup group = new JGroup();
    group.setId(groupId);
    group.setRef("GRP-" + UUID.randomUUID());
    return group;
  }
}
