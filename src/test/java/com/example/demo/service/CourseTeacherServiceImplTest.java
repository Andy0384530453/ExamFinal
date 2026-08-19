package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CourseTeacherServiceImplTest {

  private JCourseTeacherRepository courseTeacherRepository;
  private JCourseRepository courseRepository;
  private JUserRepository userRepository;
  private CourseTeacherServiceImpl service;

  @BeforeEach
  void setUp() {
    courseTeacherRepository = mock(JCourseTeacherRepository.class);
    courseRepository = mock(JCourseRepository.class);
    userRepository = mock(JUserRepository.class);
    service =
        new CourseTeacherServiceImpl(courseTeacherRepository, courseRepository, userRepository);
  }

  @Test
  void list_teachers_returns_mapped_responses() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setTeacherId(UUID.randomUUID());
    when(courseTeacherRepository.findByCourseId(courseId)).thenReturn(List.of(assignment));

    List<CourseTeacherResponse> result = service.listTeachers(courseId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).courseId()).isEqualTo(courseId);
    assertThat(result.get(0).teacherId()).isEqualTo(assignment.getTeacherId());
  }

  @Test
  void list_teachers_unknown_course_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.listTeachers(courseId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_saves_and_returns_response() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.existsById(teacherId)).thenReturn(true);
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    CourseTeacherResponse response =
        service.assign(courseId, new CourseTeacherAssignRequest(teacherId));

    assertThat(response.courseId()).isEqualTo(courseId);
    assertThat(response.teacherId()).isEqualTo(teacherId);
    assertThat(response.id()).isNotNull();
    ArgumentCaptor<JCourseTeacher> captor = ArgumentCaptor.forClass(JCourseTeacher.class);
    verify(courseTeacherRepository).save(captor.capture());
    assertThat(captor.getValue().getCourseId()).isEqualTo(courseId);
    assertThat(captor.getValue().getTeacherId()).isEqualTo(teacherId);
  }

  @Test
  void assign_already_assigned_throws_conflict() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.existsById(teacherId)).thenReturn(true);
    when(courseTeacherRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.assign(courseId, new CourseTeacherAssignRequest(teacherId)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already assigned");
    verify(courseTeacherRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void assign_unknown_course_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(
            () -> service.assign(courseId, new CourseTeacherAssignRequest(UUID.randomUUID())))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }

  @Test
  void assign_unknown_user_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(userRepository.existsById(teacherId)).thenReturn(false);

    assertThatThrownBy(() -> service.assign(courseId, new CourseTeacherAssignRequest(teacherId)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Teacher not found");
  }

  @Test
  void remove_deletes_assignment() {
    UUID courseId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(courseId);
    assignment.setTeacherId(teacherId);
    when(courseRepository.existsById(courseId)).thenReturn(true);
    when(courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId))
        .thenReturn(Optional.of(assignment));

    service.remove(courseId, teacherId);

    verify(courseTeacherRepository).delete(assignment);
  }

  @Test
  void remove_unknown_course_throws_not_found() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.existsById(courseId)).thenReturn(false);

    assertThatThrownBy(() -> service.remove(courseId, UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Course not found");
  }
}
