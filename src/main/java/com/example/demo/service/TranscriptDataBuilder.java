package com.example.demo.service;

import com.example.demo.entity.Course;
import com.example.demo.entity.Exam;
import com.example.demo.entity.Grade;
import com.example.demo.entity.TranscriptItem;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.GradeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TranscriptDataBuilder {

  private final GradeRepository gradeRepository;
  private final ExamRepository examRepository;
  private final CourseRepository courseRepository;

  public TranscriptDataBuilder(
      GradeRepository gradeRepository,
      ExamRepository examRepository,
      CourseRepository courseRepository) {
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseRepository = courseRepository;
  }

  public List<TranscriptItem> buildItems(UUID studentId, UUID promotionId) {
    return gradeRepository.findByStudentId(studentId).stream()
        .flatMap(grade -> toTranscriptItem(grade, promotionId).stream())
        .sorted(Comparator.comparing(TranscriptItem::getExamDate))
        .toList();
  }

  public Optional<TranscriptItem> toTranscriptItem(Grade grade, UUID promotionId) {
    Optional<Exam> exam = examRepository.findById(grade.getExamId());
    if (exam.isEmpty()) {
      return Optional.empty();
    }
    Optional<Course> course = courseRepository.findById(exam.get().getCourseId());
    if (course.isEmpty()) {
      return Optional.empty();
    }
    Course courseValue = course.get();
    if (promotionId != null && !promotionId.equals(courseValue.getPromotionId())) {
      return Optional.empty();
    }
    TranscriptItem item = new TranscriptItem();
    item.setCourseTitle(courseValue.getTitle());
    item.setExamDate(exam.get().getDateExam());
    item.setCoefficient(exam.get().getCoefficient());
    item.setGrade(grade.getValue());
    item.setCredits(courseValue.getCredits());
    return Optional.of(item);
  }
}
