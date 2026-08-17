package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.promotion.PromotionResultsResponse;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JStudentGroup;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JStudentGroupRepository;
import com.example.demo.repository.JUserRepository;
import java.time.Instant;
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
class PromotionResultsControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JGroupRepository groupRepository;
  @Autowired private JStudentGroupRepository studentGroupRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;

  @Test
  void admin_gets_results_over_the_three_years() {
    JUser admin = admin();
    JPromotion promotion = promotion(2030);
    JPromotion promotion2031 = promotion(2031);
    JPromotion promotion2032 = promotion(2032);
    JGroup group = group(promotion);
    JUser s1 = student();
    JUser s2 = student();
    membership(s1, group);
    membership(s2, group);
    JCourse course2030 = course(promotion);
    JCourse course2031 = course(promotion2031);
    JExam exam2030 = exam(course2030);
    JExam exam2031 = exam(course2031);
    grade(s1, exam2030, 14.0);
    grade(s2, exam2030, 8.0);
    grade(s1, exam2031, 12.0);
    grade(s2, exam2031, 16.0);

    ResponseEntity<PromotionResultsResponse> response = getResults(token(admin), promotion.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PromotionResultsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.promotionId()).isEqualTo(promotion.getId());
    assertThat(body.promotionYear()).isEqualTo(2030);
    assertThat(body.year1().year()).isEqualTo(2030);
    assertThat(body.year1().average()).isEqualTo(11.0);
    assertThat(body.year1().bestStudentId()).isEqualTo(s1.getId());
    assertThat(body.year1().failureRate()).isEqualTo(50.0);
    assertThat(body.year2().year()).isEqualTo(2031);
    assertThat(body.year2().average()).isEqualTo(14.0);
    assertThat(body.year2().bestStudentId()).isEqualTo(s2.getId());
    assertThat(body.year2().failureRate()).isEqualTo(0.0);
    assertThat(body.year3().year()).isEqualTo(2032);
    assertThat(body.year3().average()).isNull();
    assertThat(body.overallAverage()).isEqualTo(12.5);
    // Both students have an overall average >= 10 (s1: 13.0, s2: 12.0) -> 100%
    assertThat(body.graduationRate()).isEqualTo(100.0);
  }

  @Test
  void student_cannot_access_results() {
    JUser student = student();
    JPromotion promotion = promotion(2023);

    ResponseEntity<ErrorResponse> response = getResultsError(token(student), promotion.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unknown_promotion_returns_404() {
    JUser admin = admin();

    ResponseEntity<ErrorResponse> response = getResultsError(token(admin), UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private ResponseEntity<PromotionResultsResponse> getResults(String token, UUID promotionId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/results",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        PromotionResultsResponse.class);
  }

  private ResponseEntity<ErrorResponse> getResultsError(String token, UUID promotionId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/promotions/" + promotionId + "/results",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        ErrorResponse.class);
  }

  private String token(JUser user) {
    return tokenProvider.generateToken(user);
  }

  private JUser admin() {
    return user(Role.ADMIN);
  }

  private JUser student() {
    return user(Role.STUDENT);
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

  private JPromotion promotion(int year) {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + year + "-" + UUID.randomUUID());
    promotion.setYear(year);
    return promotionRepository.save(promotion);
  }

  private JGroup group(JPromotion promotion) {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP-" + UUID.randomUUID());
    group.setPromotionId(promotion.getId());
    return groupRepository.save(group);
  }

  private void membership(JUser student, JGroup group) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(student.getId());
    membership.setGroupId(group.getId());
    membership.setStartDate(Instant.parse("2023-09-01T08:00:00Z"));
    membership.setEndDate(null);
    studentGroupRepository.save(membership);
  }

  private JCourse course(JPromotion promotion) {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle("Mathematiques");
    course.setCredits(6);
    course.setPromotionId(promotion.getId());
    return courseRepository.save(course);
  }

  private JExam exam(JCourse course) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse("2023-11-15T09:00:00Z"));
    exam.setCoefficient(1.5);
    return examRepository.save(exam);
  }

  private JGrade grade(JUser student, JExam exam, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return gradeRepository.save(grade);
  }
}
