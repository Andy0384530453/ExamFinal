package com.example.demo.endpoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.conf.FacadeIT;
import com.example.demo.config.TokenProvider;
import com.example.demo.dto.grade.GradeHistoryResponse;
import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.dto.grade.GradeUpdateRequest;
import com.example.demo.entity.JCourse;
import com.example.demo.entity.JCourseTeacher;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGradeModification;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ErrorResponse;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JCourseTeacherRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeModificationRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JPromotionRepository;
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
class GradeControllerIT extends FacadeIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private TokenProvider tokenProvider;
  @Autowired private JUserRepository userRepository;
  @Autowired private JPromotionRepository promotionRepository;
  @Autowired private JCourseRepository courseRepository;
  @Autowired private JExamRepository examRepository;
  @Autowired private JGradeRepository gradeRepository;
  @Autowired private JCourseTeacherRepository courseTeacherRepository;
  @Autowired private JGradeModificationRepository gradeModificationRepository;

  @Test
  void teacher_can_list_grades_of_own_course() {
    JUser teacher = teacher();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 12.0);
    teach(teacher, course);

    ResponseEntity<List<GradeResponse>> response = listGrades(token(teacher), course.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    GradeResponse body = response.getBody().get(0);
    assertThat(body.id()).isEqualTo(grade.getId());
    assertThat(body.examId()).isEqualTo(exam.getId());
    assertThat(body.examRef()).isEqualTo(exam.getRef());
    assertThat(body.courseTitle()).isEqualTo(course.getTitle());
    assertThat(body.value()).isEqualTo(12.0);
  }

  @Test
  void teacher_cannot_list_grades_of_another_course() {
    JUser teacher = teacher();
    JCourse course = course();
    JCourse otherCourse = course();
    teach(teacher, otherCourse);

    ResponseEntity<ErrorResponse> response = listGradesError(token(teacher), course.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void admin_can_list_grades_of_any_course() {
    JUser admin = admin();
    JCourse course = course();
    grade(exam(course), 14.0);

    ResponseEntity<List<GradeResponse>> response = listGrades(token(admin), course.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void student_cannot_access_grades() {
    JUser student = student();
    JCourse course = course();

    ResponseEntity<ErrorResponse> response = listGradesError(token(student), course.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void student_can_list_own_grades() {
    JUser student = student();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 15.0, student);

    ResponseEntity<List<GradeResponse>> response =
        listStudentGrades(token(student), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    GradeResponse body = response.getBody().get(0);
    assertThat(body.id()).isEqualTo(grade.getId());
    assertThat(body.studentId()).isEqualTo(student.getId());
    assertThat(body.examId()).isEqualTo(exam.getId());
    assertThat(body.examRef()).isEqualTo(exam.getRef());
    assertThat(body.courseTitle()).isEqualTo(course.getTitle());
    assertThat(body.value()).isEqualTo(15.0);
  }

  @Test
  void student_cannot_list_another_student_grades() {
    JUser student = student();
    JUser otherStudent = student();
    JCourse course = course();
    JExam exam = exam(course);
    grade(exam, 15.0, otherStudent);

    ResponseEntity<ErrorResponse> response =
        listStudentGradesError(token(student), otherStudent.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void admin_can_list_any_student_grades() {
    JUser admin = admin();
    JUser student = student();
    JCourse course = course();
    JExam exam = exam(course);
    grade(exam, 11.0, student);

    ResponseEntity<List<GradeResponse>> response = listStudentGrades(token(admin), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).studentId()).isEqualTo(student.getId());
  }

  @Test
  void teacher_cannot_list_student_grades() {
    JUser teacher = teacher();
    JUser student = student();

    ResponseEntity<ErrorResponse> response =
        listStudentGradesError(token(teacher), student.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void teacher_updates_grade_of_own_course_writes_history() {
    JUser teacher = teacher();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);
    teach(teacher, course);

    ResponseEntity<GradeResponse> updateResponse =
        updateGrade(token(teacher), grade.getId(), new GradeUpdateRequest(14.5, "Claim accepted"));

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody().value()).isEqualTo(14.5);

    JGrade persisted = gradeRepository.findById(grade.getId()).orElseThrow();
    assertThat(persisted.getValue()).isEqualTo(14.5);
    assertThat(persisted.getModifiedBy()).isEqualTo(teacher.getId());

    List<JGradeModification> modifications =
        gradeModificationRepository.findByGradeIdOrderByModifiedAtDesc(grade.getId());
    assertThat(modifications).hasSize(1);
    JGradeModification modification = modifications.get(0);
    assertThat(modification.getOldValue()).isEqualTo(10.0);
    assertThat(modification.getNewValue()).isEqualTo(14.5);
    assertThat(modification.getReason()).isEqualTo("Claim accepted");
    assertThat(modification.getModifiedBy()).isEqualTo(teacher.getId());
    assertThat(modification.getModifiedAt()).isNotNull();
  }

  @Test
  void grade_history_returns_recorded_modifications() {
    JUser admin = admin();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);
    updateGrade(token(admin), grade.getId(), new GradeUpdateRequest(14.5, "Claim accepted"));

    ResponseEntity<List<GradeHistoryResponse>> response = getHistory(token(admin), grade.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    GradeHistoryResponse body = response.getBody().get(0);
    assertThat(body.oldValue()).isEqualTo(10.0);
    assertThat(body.newValue()).isEqualTo(14.5);
    assertThat(body.reason()).isEqualTo("Claim accepted");
    assertThat(body.modifiedBy()).isEqualTo(admin.getId());
  }

  @Test
  void update_with_same_value_returns_400() {
    JUser admin = admin();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);
    JGradeModification modification = new JGradeModification();
    modification.setId(UUID.randomUUID());
    modification.setGradeId(grade.getId());
    modification.setOldValue(10.0);
    modification.setNewValue(10.0);
    modification.setReason("nothing to change");
    modification.setModifiedAt(Instant.now());
    modification.setModifiedBy(admin.getId());
    gradeModificationRepository.save(modification);

    ResponseEntity<ErrorResponse> response =
        updateGradeError(token(admin), grade.getId(), new GradeUpdateRequest(10.0, "Claim"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already has value");
  }

  @Test
  void update_without_reason_returns_400() {
    JUser admin = admin();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);

    ResponseEntity<ErrorResponse> response =
        updateGradeError(token(admin), grade.getId(), new GradeUpdateRequest(14.5, " "));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("reason");
  }

  @Test
  void update_with_value_out_of_range_returns_400() {
    JUser admin = admin();
    JCourse course = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);

    ResponseEntity<ErrorResponse> response =
        updateGradeError(token(admin), grade.getId(), new GradeUpdateRequest(25.0, "Claim"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("value");
  }

  @Test
  void teacher_cannot_update_grade_of_another_course() {
    JUser teacher = teacher();
    JCourse course = course();
    JCourse otherCourse = course();
    JExam exam = exam(course);
    JGrade grade = grade(exam, 10.0);
    teach(teacher, otherCourse);

    ResponseEntity<ErrorResponse> response =
        updateGradeError(token(teacher), grade.getId(), new GradeUpdateRequest(14.5, "Claim"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  private ResponseEntity<List<GradeResponse>> listGrades(String token, UUID courseId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/grades",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<ErrorResponse> listGradesError(String token, UUID courseId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/courses/" + courseId + "/grades",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        ErrorResponse.class);
  }

  private ResponseEntity<List<GradeResponse>> listStudentGrades(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/grades",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<>() {});
  }

  private ResponseEntity<ErrorResponse> listStudentGradesError(String token, UUID studentId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/students/" + studentId + "/grades",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        ErrorResponse.class);
  }

  private ResponseEntity<GradeResponse> updateGrade(
      String token, UUID gradeId, GradeUpdateRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/grades/" + gradeId,
        HttpMethod.PUT,
        new HttpEntity<>(request, headers),
        GradeResponse.class);
  }

  private ResponseEntity<ErrorResponse> updateGradeError(
      String token, UUID gradeId, GradeUpdateRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/grades/" + gradeId,
        HttpMethod.PUT,
        new HttpEntity<>(request, headers),
        ErrorResponse.class);
  }

  private ResponseEntity<List<GradeHistoryResponse>> getHistory(String token, UUID gradeId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(
        "/grades/" + gradeId + "/history",
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

  private JPromotion promotion() {
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-" + UUID.randomUUID());
    promotion.setYear(2024);
    return promotionRepository.save(promotion);
  }

  private JCourse course() {
    JPromotion promotion = promotion();
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
    exam.setDateExam(Instant.parse("2024-01-01T09:00:00Z"));
    exam.setCoefficient(1.5);
    return examRepository.save(exam);
  }

  private JGrade grade(JExam exam, double value) {
    return grade(exam, value, student());
  }

  private JGrade grade(JExam exam, double value, JUser student) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(student.getId());
    grade.setExamId(exam.getId());
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return gradeRepository.save(grade);
  }

  private void teach(JUser teacher, JCourse course) {
    JCourseTeacher courseTeacher = new JCourseTeacher();
    courseTeacher.setId(UUID.randomUUID());
    courseTeacher.setCourseId(course.getId());
    courseTeacher.setTeacherId(teacher.getId());
    courseTeacherRepository.save(courseTeacher);
  }
}
