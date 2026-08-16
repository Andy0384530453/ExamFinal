package com.example.demo.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

public class LocalstackConf {

  private static final DockerImageName IMAGE = DockerImageName.parse("localstack/localstack:3.0.2");

  private final LocalStackContainer localstack =
      new LocalStackContainer(IMAGE).withServices(Service.S3, Service.SES);

  void start() {
    localstack.start();
  }

  void stop() {
    localstack.stop();
  }

  String getEndpoint() {
    return localstack.getEndpoint().toString();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.s3.endpoint", () -> localstack.getEndpoint().toString());
    registry.add("aws.ses.endpoint", () -> localstack.getEndpoint().toString());
    registry.add("aws.s3.bucket", () -> "dummy-bucket");
    registry.add("aws.ses.source", () -> "sender@test.com");
  }
}
