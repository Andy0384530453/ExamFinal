package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.graduate.GraduatesResponse;
import com.example.demo.service.GraduateService;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraduateController {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final GraduateService graduateService;

  public GraduateController(GraduateService graduateService) {
    this.graduateService = graduateService;
  }

  @GetMapping("/promotions/{id}/graduates")
  public GraduatesResponse getGraduates(@PathVariable("id") UUID id) {
    return graduateService.getGraduatesResponse(id);
  }

  @GetMapping("/promotions/{id}/graduates.xlsx")
  public ResponseEntity<byte[]> downloadGraduates(@PathVariable("id") UUID id) {
    byte[] content = graduateService.getGraduatesExcel(id);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=graduates.xlsx")
        .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
        .body(content);
  }
}
