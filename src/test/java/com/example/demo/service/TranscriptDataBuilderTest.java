package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TranscriptDataBuilderTest {

  private JGradeRepository gradeRepository;
  private JExamRepository examRepository;
  private JCourseRepository courseRepository;
  private TranscriptDataBuilder builder;

  @BeforeEach
  void setUp() {
    gradeRepository = mock(JGradeRepository.class);
    examRepository = mock(JExamRepository.class);
    courseRepository = mock(JCourseRepository.class);
    builder = new TranscriptDataBuilder(gradeRepository, examRepository, courseRepository);
  }

  @Test
  void builds_items_from_grades_through_exam_and_course() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    JCourse course = course(promotionId, "Maths", 6);
    JExam exam = exam(course, "2023-11-15T09:00:00Z", 1.5);
    JGrade grade = grade(studentId, exam.getId(), 14.5);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<JTranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(1);
    JTranscriptItem item = items.get(0);
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
    JCourse courseA = course(promotionA, "Maths", 6);
    JCourse courseB = course(promotionB, "Physique", 5);
    JExam examA = exam(courseA, "2023-11-15T09:00:00Z", 1.0);
    JExam examB = exam(courseB, "2024-11-15T09:00:00Z", 1.0);
    JGrade gradeA = grade(studentId, examA.getId(), 12.0);
    JGrade gradeB = grade(studentId, examB.getId(), 14.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(gradeA, gradeB));
    when(examRepository.findById(examA.getId())).thenReturn(Optional.of(examA));
    when(examRepository.findById(examB.getId())).thenReturn(Optional.of(examB));
    when(courseRepository.findById(courseA.getId())).thenReturn(Optional.of(courseA));
    when(courseRepository.findById(courseB.getId())).thenReturn(Optional.of(courseB));

    List<JTranscriptItem> items = builder.buildItems(studentId, promotionA);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getCourseTitle()).isEqualTo("Maths");
  }

  @Test
  void sorts_items_by_exam_date() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    JCourse course = course(promotionId, "Course", 6);
    JExam later = exam(course, "2024-01-01T09:00:00Z", 1.0);
    JExam earlier = exam(course, "2023-01-01T09:00:00Z", 1.0);
    JGrade gradeLater = grade(studentId, later.getId(), 11.0);
    JGrade gradeEarlier = grade(studentId, earlier.getId(), 15.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(gradeLater, gradeEarlier));
    when(examRepository.findById(later.getId())).thenReturn(Optional.of(later));
    when(examRepository.findById(earlier.getId())).thenReturn(Optional.of(earlier));
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<JTranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(2);
    assertThat(items)
        .extracting("examDate")
        .containsExactly(earlier.getDateExam(), later.getDateExam());
  }

  @Test
  void skips_grades_without_exam_or_course() {
    UUID studentId = UUID.randomUUID();
    UUID promotionId = UUID.randomUUID();
    JCourse course = course(promotionId, "Maths", 6);
    JExam exam = exam(course, "2023-11-15T09:00:00Z", 1.0);
    JGrade valid = grade(studentId, exam.getId(), 12.0);
    JGrade missingExam = grade(studentId, UUID.randomUUID(), 10.0);
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(valid, missingExam));
    when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
    when(examRepository.findById(missingExam.getExamId())).thenReturn(Optional.empty());
    when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));

    List<JTranscriptItem> items = builder.buildItems(studentId, promotionId);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).getCourseTitle()).isEqualTo("Maths");
  }

  private static JCourse course(UUID promotionId, String title, int credits) {
    JCourse course = new JCourse();
    course.setId(UUID.randomUUID());
    course.setRef("C-" + UUID.randomUUID());
    course.setTitle(title);
    course.setCredits(credits);
    course.setPromotionId(promotionId);
    return course;
  }

  private static JExam exam(JCourse course, String date, double coefficient) {
    JExam exam = new JExam();
    exam.setId(UUID.randomUUID());
    exam.setRef("E-" + UUID.randomUUID());
    exam.setCourseId(course.getId());
    exam.setDateExam(Instant.parse(date));
    exam.setCoefficient(coefficient);
    return exam;
  }

  private static JGrade grade(UUID studentId, UUID examId, double value) {
    JGrade grade = new JGrade();
    grade.setId(UUID.randomUUID());
    grade.setStudentId(studentId);
    grade.setExamId(examId);
    grade.setValue(value);
    grade.setModifiedAt(Instant.now());
    grade.setModifiedBy(UUID.randomUUID());
    return grade;
  }
}
