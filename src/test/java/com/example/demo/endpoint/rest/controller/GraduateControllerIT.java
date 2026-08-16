package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.globally_quoted_identifiers=true")
class GraduateControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JGroupRepository groupRepository;
  @Autowired private JStudentGroupRepository studentGroupRepository;
  @Autowired private JUserRepository userRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;

  @Test
  void promotion_page_lists_all_promotions() {
    JPromotion promotion = promotion(2024);

    ResponseEntity<String> response = restTemplate.getForEntity("/promotions", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains(promotion.getRef());
    assertThat(response.getBody()).contains("graduates.xlsx");
  }

  @Test
  void download_graduates_excel_returns_xlsx_file() {
    JPromotion promotion = promotion(2023);
    JGroup group = group(promotion);
    JUser student = student();
    studentGroup(
        student.getId(),
        group.getId(),
        Instant.parse("2023-09-01T08:00:00Z"),
        Instant.parse("2024-06-30T18:00:00Z"));
    JCourse course = course(promotion, "Mathematiques", 6);
    JExam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    grade(student, exam, 14.5);

    ResponseEntity<byte[]> response =
        restTemplate.getForEntity(
            "/promotions/" + promotion.getId() + "/graduates.xlsx", byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType())
        .hasToString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .isEqualTo("attachment; filename=graduates.xlsx");
    byte[] body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.length).isGreaterThan(0);
    assertThat(new String(body, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
  }

  @Test
  void unknown_promotion_returns_404() {
    ResponseEntity<ErrorResponse> response =
        restTemplate.getForEntity(
            "/promotions/" + UUID.randomUUID() + "/graduates.xlsx", ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ErrorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.status()).isEqualTo(404);
    assertThat(body.message()).contains("Promotion not found");
  }

  private JPromotion promotion(int year) {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + year);
    promotion.setYear(year);
    return promotionRepository.save(promotion);
  }

  private JGroup group(JPromotion promotion) {
    JGroup group = new JGroup();
    group.setId(UUID.randomUUID());
    group.setRef("GRP-" + promotion.getYear());
    group.setPromotionId(promotion.getId());
    return groupRepository.save(group);
  }

  private JStudentGroup studentGroup(UUID studentId, UUID groupId, Instant start, Instant end) {
    JStudentGroup membership = new JStudentGroup();
    membership.setId(UUID.randomUUID());
    membership.setStudentId(studentId);
    membership.setGroupId(groupId);
    membership.setStartDate(start);
    membership.setEndDate(end);
    return studentGroupRepository.save(membership);
  }

  private JUser student() {
    JUser user = new JUser();
    user.setId(UUID.randomUUID());
    user.setRef("REF-" + UUID.randomUUID());
    user.setFirstName("Lucas");
    user.setLastName("Moreau");
    user.setEmail(UUID.randomUUID() + "@school.com");
    user.setRole(Role.STUDENT);
    return userRepository.save(user);
  }

  private JCourse course(JPromotion promotion, String title, int credits) {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotion.getId());
    return courseRepository.save(course);
  }

  private JExam exam(JCourse course, String date, double coefficient) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(coefficient);
    return examRepository.save(exam);
  }

  private void grade(JUser student, JExam exam, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    gradeRepository.save(grade);
  }
}
