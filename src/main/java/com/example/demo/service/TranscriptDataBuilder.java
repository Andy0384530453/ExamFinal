package com.example.demo.service;

import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TranscriptDataBuilder {

  private final JGradeRepository gradeRepository;
  private final JExamRepository examRepository;
  private final JCourseRepository courseRepository;

  public TranscriptDataBuilder(
      JGradeRepository gradeRepository,
      JExamRepository examRepository,
      JCourseRepository courseRepository) {
    this.gradeRepository = gradeRepository;
    this.examRepository = examRepository;
    this.courseRepository = courseRepository;
  }

  public List<JTranscriptItem> buildItems(UUID studentId, UUID promotionId) {
    return gradeRepository.findByStudentId(studentId).stream()
        .flatMap(grade -> toTranscriptItem(grade, promotionId).stream())
        .sorted(Comparator.comparing(JTranscriptItem::getExamDate))
        .toList();
  }

  public Optional<JTranscriptItem> toTranscriptItem(JGrade grade, UUID promotionId) {
    Optional<JExam> exam = examRepository.findById(grade.getExamId());
    if (exam.isEmpty()) {
      return Optional.empty();
    }
    Optional<JCourse> course = courseRepository.findById(exam.get().getCourseId());
    if (course.isEmpty()) {
      return Optional.empty();
    }
    JCourse courseValue = course.get();
    if (promotionId != null && !promotionId.equals(courseValue.getPromotionId())) {
      return Optional.empty();
    }
    JTranscriptItem item = new JTranscriptItem();
    item.setCourseTitle(courseValue.getTitle());
    item.setExamDate(exam.get().getDateExam());
    item.setCoefficient(exam.get().getCoefficient());
    item.setGrade(grade.getValue());
    item.setCredits(courseValue.getCredits());
    return Optional.of(item);
  }
}
