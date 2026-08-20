package com.example.demo.mapper;

import com.example.demo.dto.grade.GradeResponse;
import com.example.demo.entity.JGrade;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

  public GradeResponse toResponse(JGrade grade, String courseTitle, UUID examId, String examRef) {
    return new GradeResponse(
        grade.getId(),
        grade.getStudentId(),
        examId,
        examRef,
        courseTitle,
        grade.getValue(),
        grade.getComment(),
        grade.getModifiedAt(),
        grade.getModifiedBy());
  }
}
