package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.coursegroup.CourseGroupAssignRequest;
import com.example.demo.dto.coursegroup.CourseGroupResponse;
import com.example.demo.dto.courseteacher.CourseTeacherAssignRequest;
import com.example.demo.dto.courseteacher.CourseTeacherResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JCourseGroupRepository;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class CourseAssignmentControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JGroupRepository groupRepository;
  @Autowired private JCourseTeacherRepository courseTeacherRepository;
  @Autowired private JCourseGroupRepository courseGroupRepository;

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
    JUser teacher = teacher();
    JUser otherTeacher = teacher();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response =
        assignTeacherError(token(teacher), course.getId(), otherTeacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
  void remove_unassigned_teacher_returns_404() {
    JUser admin = admin();
    JUser teacher = teacher();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response =
        removeTeacherError(token(admin), course.getId(), teacher.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void teacher_of_course_assigns_group() {
    JUser teacher = teacher();
    JCourse course = course();
    JGroup group = group();
    teach(teacher, course);

    ResponseEntity<CourseGroupResponse> response =
        assignGroup(token(teacher), course.getId(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    CourseGroupResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.courseId()).isEqualTo(course.getId());
    assertThat(body.groupId()).isEqualTo(group.getId());
    assertThat(courseGroupRepository.findByCourseIdAndGroupId(course.getId(), group.getId()))
        .isPresent();
  }

  @Test
  void teacher_of_another_course_cannot_assign_group() {
    JUser teacher = teacher();
    JCourse course = course();
    JCourse otherCourse = course();
    JGroup group = group();
    teach(teacher, otherCourse);

    ResponseEntity<ErrorResponse> response =
        assignGroupError(token(teacher), course.getId(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void duplicate_group_assignment_returns_409() {
    JUser admin = admin();
    JCourse course = course();
    JGroup group = group();
    assignGroup(token(admin), course.getId(), group.getId());

    ResponseEntity<ErrorResponse> response =
        assignGroupError(token(admin), course.getId(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().message()).contains("already associated");
  }

  @Test
  void admin_removes_group_from_course() {
    JUser admin = admin();
    JCourse course = course();
    JGroup group = group();
    assignGroup(token(admin), course.getId(), group.getId());

    ResponseEntity<Void> response = removeGroup(token(admin), course.getId(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(courseGroupRepository.findByCourseIdAndGroupId(course.getId(), group.getId()))
        .isEmpty();
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

  @Test
  void unknown_group_returns_404() {
    JUser admin = admin();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response =
        assignGroupError(token(admin), course.getId(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("Group not found");
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

  private ResponseEntity<ErrorResponse> removeTeacherError(
      String token, UUID courseId, UUID teacherId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/teachers/" + teacherId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        ErrorResponse.class);
  }

  private ResponseEntity<CourseGroupResponse> assignGroup(
      String token, UUID courseId, UUID groupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/groups",
        HttpMethod.POST,
        new HttpEntity<>(new CourseGroupAssignRequest(groupId), headers),
        CourseGroupResponse.class);
  }

  private ResponseEntity<ErrorResponse> assignGroupError(
      String token, UUID courseId, UUID groupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/groups",
        HttpMethod.POST,
        new HttpEntity<>(new CourseGroupAssignRequest(groupId), headers),
        ErrorResponse.class);
  }

  private ResponseEntity<Void> removeGroup(String token, UUID courseId, UUID groupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/groups/" + groupId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Void.class);
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

  private JGroup group() {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP-" + UUID.randomUUID());
    group.setPromotionId(promotion().getId());
    return groupRepository.save(group);
  }

  private void teach(JUser teacher, JCourse course) {
    JCourseTeacher assignment = new JCourseTeacher();
    assignment.setId(UUID.randomUUID());
    assignment.setCourseId(course.getId());
    assignment.setTeacherId(teacher.getId());
    courseTeacherRepository.save(assignment);
  }
}
