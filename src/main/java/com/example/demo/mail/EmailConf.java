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
  private final String sesEndpoint;

  public EmailConf(
      @Value("${aws.ses.source}") String sesSource,
      @Value("${aws.ses.endpoint:}") String sesEndpoint) {
    this.sesSource = sesSource;
    this.sesEndpoint = sesEndpoint;
  }

  @Bean
  public SesClient getSesClient() {
    var builder = SesClient.builder().region(Region.EU_WEST_3);
    if (sesEndpoint != null && !sesEndpoint.isBlank()) {
      builder.endpointOverride(URI.create(sesEndpoint));
    }
    return builder.build();
  }
}
