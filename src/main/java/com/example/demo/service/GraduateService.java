package com.example.demo.service;

import com.example.demo.dto.graduate.GraduateResponse;
import com.example.demo.dto.graduate.GraduatesResponse;
import java.util.List;
import java.util.UUID;

public interface GraduateService {

  List<GraduateResponse> getGraduates(UUID promotionId);

  GraduatesResponse getGraduatesResponse(UUID promotionId);

  byte[] getGraduatesExcel(UUID promotionId);
}
