package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.group.StudentGroupChangeRequest;
import com.example.demo.dto.group.StudentGroupResponse;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import java.time.Instant;
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
class StudentGroupControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JGroupRepository groupRepository;
  @Autowired private JStudentGroupRepository studentGroupRepository;

  @Test
  void admin_gets_student_group_history() {
    JUser student = student();
    JGroup group = group();
    membership(student.getId(), group.getId(), null);
    JUser admin = admin();

    ResponseEntity<List<StudentGroupResponse>> response = getHistory(token(admin), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    StudentGroupResponse body = response.getBody().get(0);
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.groupId()).isEqualTo(group.getId());
    assertThat(body.endDate()).isNull();
  }

  @Test
  void student_gets_own_group_history() {
    JUser student = student();
    JGroup group = group();
    membership(student.getId(), group.getId(), null);

    ResponseEntity<List<StudentGroupResponse>> response =
        getHistory(token(student), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).groupId()).isEqualTo(group.getId());
  }

  @Test
  void student_cannot_get_another_student_history() {
    JUser student = student();
    JUser otherStudent = student();

    ResponseEntity<ErrorResponse> response = getHistoryError(token(student), otherStudent.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void unknown_student_history_returns_404() {
    JUser admin = admin();

    ResponseEntity<ErrorResponse> response = getHistoryError(token(admin), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).contains("Student not found");
  }

  @Test
  void admin_changes_student_group_closes_previous_assignment() {
    JUser student = student();
    JGroup oldGroup = group();
    JGroup newGroup = group();
    JStudentGroup previous = membership(student.getId(), oldGroup.getId(), null);
    JUser admin = admin();

    ResponseEntity<StudentGroupResponse> response =
        changeGroup(token(admin), student.getId(), newGroup.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    StudentGroupResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.groupId()).isEqualTo(newGroup.getId());
    assertThat(body.endDate()).isNull();

    JStudentGroup closed = studentGroupRepository.findById(previous.getId()).orElseThrow();
    assertThat(closed.getEndDate()).isNotNull();

    JStudentGroup created = studentGroupRepository.findById(body.id()).orElseThrow();
    assertThat(created.getGroupId()).isEqualTo(newGroup.getId());
    assertThat(created.getStartDate()).isNotNull();
  }

  @Test
  void change_group_unknown_student_returns_404() {
    JUser admin = admin();
    JGroup group = group();

    ResponseEntity<ErrorResponse> response =
        changeGroupError(token(admin), UUID.randomUUID(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("Student not found");
  }

  @Test
  void change_group_unknown_group_returns_404() {
    JUser admin = admin();
    JUser student = student();

    ResponseEntity<ErrorResponse> response =
        changeGroupError(token(admin), student.getId(), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("Group not found");
  }

  @Test
  void student_cannot_change_group() {
    JUser student = student();
    JGroup group = group();

    ResponseEntity<ErrorResponse> response =
        changeGroupError(token(student), student.getId(), group.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private ResponseEntity<List<StudentGroupResponse>> getHistory(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<ErrorResponse> getHistoryError(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        ErrorResponse.class);
  }

  private ResponseEntity<StudentGroupResponse> changeGroup(
      String token, UUID studentId, UUID groupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups",
        HttpMethod.POST,
        new HttpEntity<>(new StudentGroupChangeRequest(groupId, null), headers),
        StudentGroupResponse.class);
  }

  private ResponseEntity<ErrorResponse> changeGroupError(
      String token, UUID studentId, UUID groupId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/groups",
        HttpMethod.POST,
        new HttpEntity<>(new StudentGroupChangeRequest(groupId, null), headers),
        ErrorResponse.class);
  }

  private String token(JUser user) {
    return tokenProvider.generateToken(user);
  }

  private JUser student() {
    return user(Role.STUDENT);
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

  private JGroup group() {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP-" + UUID.randomUUID());
    group.setPromotionId(promotion().getId());
    return groupRepository.save(group);
  }

  private JStudentGroup membership(UUID studentId, UUID groupId, Instant endDate) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(studentId);
    membership.setGroupId(groupId);
    membership.setStartDate(Instant.parse("2024-09-01T08:00:00Z"));
    membership.setEndDate(endDate);
    return studentGroupRepository.save(membership);
  }
}
