package com.example.demo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.dto.transcript.TranscriptItemResponse;
import com.example.demo.dto.transcript.TranscriptResponse;
import com.example.demo.entity.JTranscript;
import com.example.demo.entity.JTranscriptItem;
import com.example.demo.enums.TranscriptStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptMapperTest {

  private final TranscriptMapper mapper = new TranscriptMapper();

  @Test
  void maps_transcript_item_entity_to_response() {
    JTranscriptItem item = new JTranscriptItem();
    item.setCourseTitle("Mathematiques");
    item.setExamDate(Instant.parse("2023-11-15T09:00:00Z"));
    item.setCoefficient(1.5);
    item.setGrade(14.5);
    item.setCredits(6);

    TranscriptItemResponse response = mapper.toItemResponse(item);

    assertThat(response.courseTitle()).isEqualTo("Mathematiques");
    assertThat(response.examDate()).isEqualTo(Instant.parse("2023-11-15T09:00:00Z"));
    assertThat(response.coefficient()).isEqualTo(1.5);
    assertThat(response.grade()).isEqualTo(14.5);
    assertThat(response.credits()).isEqualTo(6);
  }

  @Test
  void maps_transcript_item_with_nullable_grade_to_response() {
    JTranscriptItem item = new JTranscriptItem();
    item.setCourseTitle("Physique");
    item.setExamDate(Instant.parse("2023-12-01T09:00:00Z"));
    item.setCoefficient(1.0);
    item.setGrade(null);
    item.setCredits(5);

    TranscriptItemResponse response = mapper.toItemResponse(item);

    assertThat(response.grade()).isNull();
  }

  @Test
  void maps_transcript_entity_to_response_with_items() {
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(UUID.randomUUID());
    transcript.setPromotionId(UUID.randomUUID());
    transcript.setStatus(TranscriptStatus.GENERATED);
    transcript.setPdfUrl("https://s3.example/transcript.pdf");
    transcript.setEmail("student@school.com");
    transcript.setGeneratedAt(Instant.parse("2024-01-01T10:00:00Z"));

    TranscriptItemResponse item = mapper.toItemResponse(transcriptItem("Mathematiques", 14.5, 6));

    TranscriptResponse response = mapper.toResponse(transcript, List.of(item));

    assertThat(response.id()).isEqualTo(transcript.getId());
    assertThat(response.studentId()).isEqualTo(transcript.getStudentId());
    assertThat(response.promotionId()).isEqualTo(transcript.getPromotionId());
    assertThat(response.status()).isEqualTo(TranscriptStatus.GENERATED);
    assertThat(response.pdfUrl()).isEqualTo("https://s3.example/transcript.pdf");
    assertThat(response.email()).isEqualTo("student@school.com");
    assertThat(response.generatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
    assertThat(response.items()).containsExactly(item);
  }

  @Test
  void maps_global_transcript_with_null_promotion_and_nullable_fields() {
    JTranscript transcript = new JTranscript();
    transcript.setId(UUID.randomUUID());
    transcript.setStudentId(UUID.randomUUID());
    transcript.setPromotionId(null);
    transcript.setStatus(TranscriptStatus.PENDING);
    transcript.setPdfUrl(null);
    transcript.setEmail(null);
    transcript.setGeneratedAt(null);

    TranscriptResponse response = mapper.toResponse(transcript, List.of());

    assertThat(response.promotionId()).isNull();
    assertThat(response.pdfUrl()).isNull();
    assertThat(response.email()).isNull();
    assertThat(response.generatedAt()).isNull();
    assertThat(response.items()).isEmpty();
  }

  private static JTranscriptItem transcriptItem(String title, Double grade, int credits) {
    JTranscriptItem item = new JTranscriptItem();
    item.setCourseTitle(title);
    item.setExamDate(Instant.parse("2023-11-15T09:00:00Z"));
    item.setCoefficient(1.0);
    item.setGrade(grade);
    item.setCredits(credits);
    return item;
  }
}
