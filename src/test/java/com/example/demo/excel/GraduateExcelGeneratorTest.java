package com.example.demo.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.entity.JPromotion;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraduateExcelGeneratorTest {

  @Test
  void generates_valid_xlsx_file() {
    GraduateExcelGenerator generator = new GraduateExcelGenerator();
    JPromotion promotion = new JPromotion();
    promotion.setId(UUID.randomUUID());
    promotion.setRef("P-2024");
    promotion.setYear(2024);
    GraduateResponse graduate =
        new GraduateResponse(UUID.randomUUID(), "Lucas", "Moreau", "lucas@school.com", 13.5);

    byte[] content = generator.generate(promotion, List.of(graduate));

    assertThat(content.length).isGreaterThan(0);
    assertThat(new String(content, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
  }
}
