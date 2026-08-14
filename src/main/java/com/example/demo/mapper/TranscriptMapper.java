package com.example.demo.mapper;

import com.example.demo.dto.transcript.TranscriptItemResponse;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.entity.Transcript;
import com.example.demo.entity.TranscriptItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranscriptMapper {

  public TranscriptItemResponse toItemResponse(TranscriptItem item) {
    return new TranscriptItemResponse(
        item.getCourseTitle(),
        item.getExamDate(),
        item.getCoefficient(),
        item.getGrade(),
        item.getCredits());
  }

  public TranscriptResponse toResponse(Transcript transcript, List<TranscriptItemResponse> items) {
    return new TranscriptResponse(
        transcript.getId(),
        transcript.getStudentId(),
        transcript.getPromotionId(),
        transcript.getStatus(),
        transcript.getPdfUrl(),
        transcript.getEmail(),
        transcript.getGeneratedAt(),
        items);
  }
}
