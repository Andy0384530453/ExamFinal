package com.example.demo.validator;

import com.example.demo.entity.JCourse;
import com.example.demo.entity.JExam;
import com.example.demo.entity.JGrade;
import com.example.demo.entity.JGroup;
import com.example.demo.entity.JPromotion;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.JCourseRepository;
import com.example.demo.repository.JExamRepository;
import com.example.demo.repository.JGradeRepository;
import com.example.demo.repository.JGroupRepository;
import com.example.demo.repository.JPromotionRepository;
import com.example.demo.repository.JTranscriptRepository;
import com.example.demo.repository.JUserRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EntityValidator {

  private final JCourseRepository courseRepository;
  private final JGroupRepository groupRepository;
  private final JUserRepository userRepository;
  private final JPromotionRepository promotionRepository;
  private final JExamRepository examRepository;
  private final JGradeRepository gradeRepository;
  private final JTranscriptRepository transcriptRepository;

  public EntityValidator(
      JCourseRepository courseRepository,
      JGroupRepository groupRepository,
      JUserRepository userRepository,
      JPromotionRepository promotionRepository,
      JExamRepository examRepository,
      JGradeRepository gradeRepository,
      JTranscriptRepository transcriptRepository) {
    this.courseRepository = courseRepository;
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.promotionRepository = promotionRepository;
    this.examRepository = examRepository;
    this.gradeRepository = gradeRepository;
    this.transcriptRepository = transcriptRepository;
  }

  public JCourse requireCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
  }

  public JGroup requireGroup(UUID groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
  }

  public JUser requireTeacher(UUID teacherId) {
    return userRepository
        .findById(teacherId)
        .filter(user -> user.getRole() == Role.TEACHER)
        .orElseThrow(
            () -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
  }

  public void assertStudentExists(UUID studentId) {
    if (!userRepository.existsById(studentId)) {
      throw new ResourceNotFoundException("Student not found with id: " + studentId);
    }
  }

  public JUser requireStudent(UUID studentId) {
    return userRepository
        .findById(studentId)
        .filter(user -> user.getRole() == Role.STUDENT)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student not found with id: " + studentId));
  }

  public JPromotion requirePromotion(UUID promotionId) {
    return promotionRepository
        .findById(promotionId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Promotion not found with id: " + promotionId));
  }

  public void assertPromotionExists(UUID promotionId) {
    if (promotionId != null && !promotionRepository.existsById(promotionId)) {
      throw new ResourceNotFoundException("Promotion not found with id: " + promotionId);
    }
  }

  public JExam requireExam(UUID examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new ResourceNotFoundException("Exam not found with id: " + examId));
  }

  public JGrade requireGrade(UUID gradeId) {
    return gradeRepository
        .findById(gradeId)
        .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
  }

  public JTranscript requireTranscript(UUID transcriptId) {
    return transcriptRepository
        .findById(transcriptId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Transcript not found with id: " + transcriptId));
  }
}
