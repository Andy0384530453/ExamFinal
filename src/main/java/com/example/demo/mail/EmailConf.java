package com.example.demo.mail;

import com.example.demo.PojaGenerated;
import java.net.URI;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@PojaGenerated
@Configuration
public class EmailConf {

  @Getter private final String sesSource;
  private final Region region;
  private final String endpoint;

  public EmailConf(
      @Value("${aws.ses.source:noreply@poja.io}") String sesSource,
      @Value("eu-west-3") Region region,
      @Value("${aws.ses.endpoint:}") String endpoint) {
    this.sesSource = sesSource;
    this.region = region;
    this.endpoint = endpoint;
  }

  @Bean
  public SesClient getSesClient() {
    var builder = SesClient.builder().region(region);
    if (!endpoint.isEmpty()) {
      builder.endpointOverride(URI.create(endpoint));
    }
    return builder.build();
  }
}
