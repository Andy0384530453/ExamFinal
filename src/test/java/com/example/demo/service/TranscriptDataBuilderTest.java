package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.Course;
import com.example.demo.entity.Exam;
import com.example.demo.entity.Grade;
import com.example.demo.entity.TranscriptItem;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.GradeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TranscriptDataBuilderTest {

  private GradeRepository gradeRepository;
  private ExamRepository examRepository;
  private CourseRepository courseRepository;
  private TranscriptDataBuilder builder;

  @BeforeEach
  void setUp() {
    gradeRepository = mock(GradeRepository.class);
    examRepository = mock(ExamRepository.class);
    courseRepository = mock(CourseRepository.class);
    builder = new TranscriptDataBuilder(gradeRepository, examRepository, courseRepository);
  }

  @Test
  void builds_items_from_grades_through_exam_and_course() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    Course course = course(promotionId, "Maths", 6);
    Exam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    Grade grade = grade(studentId, exam.getId(), 14.5);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<TranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(1);
    TranscriptItem item = items.get(0);
    assertThat(item.getCourseTitle()).isEqualTo("Maths");
    assertThat(item.getExamDate()).isEqualTo(exam.getDateExam());
    assertThat(item.getCoefficient()).isEqualTo(1.5);
    assertThat(item.getGrade()).isEqualTo(14.5);
    assertThat(item.getCredits()).isEqualTo(6);
  }

  @Test
  void filters_items_by_promotion() {
    UUID studentId = UUID.randomUUID();
    UUID promotionA = UUID.randomUUID();
    UUID promotionB = UUID.randomUUID();
    Course courseA = course(promotionA, "Maths", 6);
    Course courseB = course(promotionB, "Physique", 5);
    Exam examA = exam(courseA, "2023-11-15T09:00:00Z", 1.0);
    Exam examB = exam(courseB, "2024-11-15T09:00:00Z", 1.0);
    Grade gradeA = grade(studentId, examA.getId(), 12.0);
    Grade gradeB = grade(studentId, examB.getId(), 14.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(gradeA, gradeB));
    when(examRepository.findById(examA.getId())).thenReturn(Optional.of(examA));
    when(examRepository.findById(examB.getId())).thenReturn(Optional.of(examB));
    when(courseRepository.findById(courseA.getId())).thenReturn(Optional.of(courseA));
    when(courseRepository.findById(courseB.getId())).thenReturn(Optional.of(courseB));

    List<TranscriptItem> items = builder.buildItems(studentId, promotionA);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getCourseTitle()).isEqualTo("Maths");
  }

  @Test
  void sorts_items_by_exam_date() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    Course course = course(promotionId, "Course", 6);
    Exam later = exam(course, "2024-01-01T09:00:00Z", 1.0);
    Exam earlier = exam(course, "2023-01-01T09:00:00Z", 1.0);
    Grade gradeLater = grade(studentId, later.getId(), 11.0);
    Grade gradeEarlier = grade(studentId, earlier.getId(), 15.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(gradeLater, gradeEarlier));
    when(examRepository.findById(later.getId())).thenReturn(Optional.of(later));
    when(examRepository.findById(earlier.getId())).thenReturn(Optional.of(earlier));
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<TranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(2);
    assertThat(items)
        .extracting("examDate")
        .containsExactly(earlier.getDateExam(), later.getDateExam());
  }

  @Test
  void skips_grades_without_exam_or_course() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    Course course = course(promotionId, "Maths", 6);
    Exam exam = exam(course, "2023-11-15T09:00:00Z", 1.0);
    Grade valid = grade(studentId, exam.getId(), 12.0);
    Grade missingExam = grade(studentId, UUID.randomUUID(), 10.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(valid, missingExam));
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(examRepository.findById(missingExam.getExamId())).thenReturn(Optional.empty());
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<TranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getCourseTitle()).isEqualTo("Maths");
  }

  private static Course course(UUID promotionId, String title, int credits) {
    Course course = new Course();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotionId);
    return course;
  }

  private static Exam exam(Course course, String date, double coefficient) {
    Exam exam = new Exam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(coefficient);
    return exam;
  }

  private static Grade grade(UUID studentId, UUID examId, double value) {
    Grade grade = new Grade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(studentId);
    grade.setExamId(examId);
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return grade;
  }
}
