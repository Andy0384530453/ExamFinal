package com.example.demo.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.JTranscriptItem;
import com.example.demo.entity.JUser;
import com.example.demo.enums.Role;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptPdfGeneratorTest {

  @Test
  void generates_valid_pdf_file() throws IOException {
    TranscriptPdfGenerator generator = new TranscriptPdfGenerator();
    JUser student = new JUser();
    student.setId(UUID.randomUUID());
    student.setFirstName("Lucas");
    student.setLastName("Moreau");
    student.setEmail("student@school.com");
    student.setRole(Role.STUDENT);

    JTranscriptItem item = new JTranscriptItem();
    item.setId(UUID.randomUUID());
    item.setTranscriptId(UUID.randomUUID());
    item.setCourseTitle("Maths");
    item.setExamDate(Instant.parse("2024-01-01T09:00:00Z"));
    item.setCoefficient(1.5);
    item.setGrade(14.5);
    item.setCredits(6);

    File pdf = generator.generate(UUID.randomUUID(), student, List.of(item));

    assertThat(pdf).exists();
    assertThat(pdf.length()).isGreaterThan(0);
    byte[] header = Files.readAllBytes(pdf.toPath());
    assertThat(new String(header, 0, 5)).isEqualTo("%PDF-");
  }
}
