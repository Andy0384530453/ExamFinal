package com.example.demo.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("app.security.jwt-secret", () -> "test-jwt-secret-for-integration-tests");
  }
}
