package com.example.demo.endpoint.rest.controller;

import com.example.demo.repository.JPromotionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PromotionPageController {

  private final JPromotionRepository promotionRepository;

  public PromotionPageController(JPromotionRepository promotionRepository) {
    this.promotionRepository = promotionRepository;
  }

  @GetMapping("/promotions")
  public String promotions(Model model) {
    model.addAttribute(
        "promotions", promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "year")));
    return "promotions";
  }
}
