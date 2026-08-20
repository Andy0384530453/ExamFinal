package com.example.demo.service;

import org.springframework.stereotype.Component;

@Component
public class Rounder {

  public double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  public double roundToPercentage(double numerator, double denominator) {
    return round(numerator * 100.0 / denominator);
  }
}
