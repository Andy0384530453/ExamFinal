package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.config.GradeAccessGuard;
import com.example.demo.dto.course.CourseGroupResponse;
import com.example.demo.dto.course.CourseTeacherResponse;
import com.example.demo.entity.JCourseGroup;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

class CourseAssignmentServiceImplTest {

  private GradeAccessGuard accessGuard;
  private JCourseRepository courseRepository;
  private JGroupRepository groupRepository;
  private JUserRepository userRepository;
  private JCourseTeacherRepository courseTeacherRepository;
  private JCourseGroupRepository courseGroupRepository;
  private CourseAssignmentServiceImpl service;

  @BeforeEach
  void setUp() {
    accessGuard = mock(GradeAccessGuard.class);
    courseRepository = mock(JCourseRepository.class);
    groupRepository = mock(JGroupRepository.class);
    userRepository = mock(JUserRepository.class);
    courseTeacherRepository = mock(JCourseTeacherRepository.class);
    courseGroupRepository = mock(JCourseGroupRepository.class);
    service =
        new CourseAssignmentServiceImpl(
            accessGuard,
            courseRepository,
            groupRepository,
            userRepository,
            courseTeacherRepository,
            courseGroupRepository);
  }

  @Test
  void assign_teacher_saves_and_returns_response() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, Role.TEACHER)));
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    CourseTeacherResponse response = service.assignTeacher(courseId, teacherId);

    assertThat(response.courseId()).isEqualTo(courseId);
    assertThat(response.teacherId()).isEqualTo(teacherId);
    assertThat(response.id()).isNotNull();
    ArgumentCaptor<JCourseTeacher> captor = ArgumentCaptor.forClass(JCourseTeacher.class);
    verify(courseTeacherRepository).save(captor.capture());
    assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
    assertThat(captor.getValue().getTeacherId()).isEqualTo(teacherId);
  }

  @Test
  void assign_teacher_already_assigned_throws_conflict() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(user(teacherId, Role.TEACHER)));
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.assignTeacher(courseId, teacherId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already assigned");
    verify(courseTeacherRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void assign_teacher_unknown_course_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.assignTeacher(courseId, UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_non_teacher_user_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.findById(studentId)).thenReturn(Optional.of(user(studentId, Role.STUDENT)));

    assertThatThrownBy(() -> service.assignTeacher(courseId, studentId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Teacher not found");
  }

  @Test
  void remove_teacher_deletes_assignment() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setTeacherId(teacherId);
    when(courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId))
        .thenReturn(Optional.of(assignment));

    service.removeTeacher(courseId, teacherId);

    verify(courseTeacherRepository).delete(assignment);
  }

  @Test
  void remove_teacher_not_assigned_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.removeTeacher(courseId, teacherId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("is not assigned");
  }

  @Test
  void assign_group_saves_and_returns_response() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(groupRepository.existsById(groupId)).thenReturn(true);
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.empty());

    CourseGroupResponse response = service.assignGroup(courseId, groupId, jwt);

    assertThat(response.courseId()).isEqualTo(courseId);
    assertThat(response.groupId()).isEqualTo(groupId);
    verify(accessGuard).checkAdminOrTeacherOfCourse(courseId, jwt);
    ArgumentCaptor<JCourseGroup> captor = ArgumentCaptor.forClass(JCourseGroup.class);
    verify(courseGroupRepository).save(captor.capture());
    assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
    assertThat(captor.getValue().getGroupId()).isEqualTo(groupId);
  }

  @Test
  void assign_group_duplicate_throws_conflict() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JCourseGroup existing = new JCourseGroup();
    existing.setId(UUID.randomUUID());
    existing.setCourseId(courseId);
    existing.setGroupId(groupId);
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(groupRepository.existsById(groupId)).thenReturn(true);
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.assignGroup(courseId, groupId, jwt))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already associated");
    verify(courseGroupRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void assign_group_unknown_course_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.assignGroup(courseId, UUID.randomUUID(), jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_group_unknown_group_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(groupRepository.existsById(groupId)).thenReturn(false);

    assertThatThrownBy(() -> service.assignGroup(courseId, groupId, jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Group not found");
  }

  @Test
  void remove_group_deletes_assignment() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JCourseGroup assignment = new JCourseGroup();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setGroupId(groupId);
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.of(assignment));

    service.removeGroup(courseId, groupId, jwt);

    verify(accessGuard).checkAdminOrTeacherOfCourse(courseId, jwt);
    verify(courseGroupRepository).delete(assignment);
  }

  @Test
  void remove_group_not_associated_throws_not_found() {
    Jwt jwt = mock(Jwt.class);
    UUID courseId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    when(courseGroupRepository.findByCourseIdAndGroupId(courseId, groupId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.removeGroup(courseId, groupId, jwt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("is not associated");
  }

  private static JUser user(UUID id, Role role) {
    JUser user = new JUser();
    user.setId(id);
    user.setRef("REF-" + id);
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail(id + "@school.com");
    user.setRole(role);
    return user;
  }
}
