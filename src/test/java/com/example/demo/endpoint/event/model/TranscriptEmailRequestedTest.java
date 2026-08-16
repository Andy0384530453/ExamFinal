package com.example.demo.endpoint.event.model;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptEmailRequestedTest {

  @Test
  void event_round_trips_through_worker_deserialization() throws Exception {
    ObjectMapper om = new ObjectMapper();
    om.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    om.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    om.findAndRegisterModules();
    TranscriptEmailRequested event =
        TranscriptEmailRequested.builder().transcriptId(UUID.randomUUID()).build();

    String serialized = om.writeValueAsString(event);
    Map<String, Object> detail = om.readValue(serialized, new TypeReference<>() {});
    TranscriptEmailRequested decoded = om.convertValue(detail, TranscriptEmailRequested.class);

    assertThat(decoded.getTranscriptId()).isEqualTo(event.getTranscriptId());
    assertThat(decoded.maxConsumerDuration().toSeconds()).isEqualTo(120);
  }
}
