package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JUserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class CourseTeacherControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JCourseTeacherRepository courseTeacherRepository;

  @Test
  void admin_assigns_teacher_to_course() {
    JUser admin = admin();
    JUser teacher = teacher();
    JCourse course = course();

    ResponseEntity<CourseTeacherResponse> response =
        assignTeacher(token(admin), course.getId(), teacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    CourseTeacherResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.courseId()).isEqualTo(course.getId());
    assertThat(body.teacherId()).isEqualTo(teacher.getId());
    assertThat(
            courseTeacherRepository.existsByTeacherIdAndCourseId(teacher.getId(), course.getId()))
        .isTrue();
  }

  @Test
  void duplicate_teacher_assignment_returns_409() {
    JUser admin = admin();
    JUser teacher = teacher();
    JCourse course = course();
    assignTeacher(token(admin), course.getId(), teacher.getId());

    ResponseEntity<ErrorResponse> response =
        assignTeacherError(token(admin), course.getId(), teacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().message()).contains("already assigned");
  }

  @Test
  void teacher_cannot_assign_teacher() {
    JUser teacher1 = teacher();
    JUser teacher2 = teacher();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response =
        assignTeacherError(token(teacher1), course.getId(), teacher2.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_teachers_returns_assigned() {
    JUser admin = admin();
    JUser teacher = teacher();
    JCourse course = course();
    assignTeacher(token(admin), course.getId(), teacher.getId());

    ResponseEntity<List<CourseTeacherResponse>> response =
        listTeachers(token(admin), course.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).teacherId()).isEqualTo(teacher.getId());
  }

  @Test
  void admin_removes_teacher_from_course() {
    JUser admin = admin();
    JUser teacher = teacher();
    JCourse course = course();
    assignTeacher(token(admin), course.getId(), teacher.getId());

    ResponseEntity<Void> response = removeTeacher(token(admin), course.getId(), teacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(
            courseTeacherRepository.existsByTeacherIdAndCourseId(teacher.getId(), course.getId()))
        .isFalse();
  }

  @Test
  void unknown_course_returns_404() {
    JUser admin = admin();
    JUser teacher = teacher();

    ResponseEntity<ErrorResponse> response =
        assignTeacherError(token(admin), UUID.randomUUID(), teacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("Course not found");
  }

  @Test
  void unknown_teacher_returns_404() {
    JUser admin = admin();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response =
        assignTeacherError(token(admin), course.getId(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("Teacher not found");
  }

  private ResponseEntity<CourseTeacherResponse> assignTeacher(
      String token, UUID courseId, UUID teacherId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers",
        HttpMethod.POST,
        new HttpEntity<>(new CourseTeacherAssignRequest(teacherId), headers),
        CourseTeacherResponse.class);
  }

  private ResponseEntity<ErrorResponse> assignTeacherError(
      String token, UUID courseId, UUID teacherId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers",
        HttpMethod.POST,
        new HttpEntity<>(new CourseTeacherAssignRequest(teacherId), headers),
        ErrorResponse.class);
  }

  private ResponseEntity<Void> removeTeacher(String token, UUID courseId, UUID teacherId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers/" + teacherId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Void.class);
  }

  private ResponseEntity<List<CourseTeacherResponse>> listTeachers(String token, UUID courseId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  private String token(JUser user) {
    return tokenProvider.generateToken(user);
  }

  private JUser teacher() {
    return user(Role.TEACHER);
  }

  private JUser admin() {
    return user(Role.ADMIN);
  }

  private JUser user(Role role) {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("First");
    user.setLastName("Last");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(role);
    return userRepository.save(user);
  }

  private JPromotion promotion() {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + UUID.randomUUID());
    promotion.setYear(2024);
    return promotionRepository.save(promotion);
  }

  private JCourse course() {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle("Mathematiques");
    course.setCredits(6);
    course.setPromotionId(promotion().getId());
    return courseRepository.save(course);
  }
}
