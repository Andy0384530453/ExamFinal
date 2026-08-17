package com.example.demo.endpoint.rest.controller;

import com.example.demo.dto.promotion.PromotionResultsResponse;
import com.example.demo.service.PromotionResultsService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromotionResultsController {

  private final PromotionResultsService promotionResultsService;

  public PromotionResultsController(PromotionResultsService promotionResultsService) {
    this.promotionResultsService = promotionResultsService;
  }

  @GetMapping("/promotions/{id}/results")
  public PromotionResultsResponse getPromotionResults(@PathVariable("id") UUID id) {
    return promotionResultsService.getPromotionResults(id);
  }
}
