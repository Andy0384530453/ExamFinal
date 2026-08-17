package com.example.demo.service;

import com.example.demo.dto.promotion.PromotionResultsResponse;
import java.util.UUID;

public interface PromotionResultsService {

  PromotionResultsResponse getPromotionResults(UUID promotionId);
}
