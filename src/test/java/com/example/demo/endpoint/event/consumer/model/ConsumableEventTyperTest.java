package com.example.demo.endpoint.event.consumer.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.example.demo.endpoint.event.EventConf;
import com.example.demo.endpoint.event.model.TranscriptEmailRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsumableEventTyperTest {

  @Test
  void apply_converts_valid_sqs_messages_to_consumable_events() throws Exception {
    ObjectMapper om = new ObjectMapper();
    EventConf eventConf = mock(EventConf.class);
    ConsumableEventTyper typer = new ConsumableEventTyper(om, eventConf);

    TranscriptEmailRequested event =
        TranscriptEmailRequested.builder().transcriptId(UUID.randomUUID()).build();

    Map<String, Object> detail = new HashMap<>();
    detail.put("transcriptId", event.getTranscriptId());
    detail.put("attemptNb", 0);

    Map<String, Object> envelope = new HashMap<>();
    envelope.put("detail-type", TranscriptEmailRequested.class.getName());
    envelope.put("detail", detail);

    SQSMessage message = new SQSMessage();
    message.setBody(om.writeValueAsString(envelope));
    Map<String, String> attributes = new HashMap<>();
    attributes.put("ApproximateReceiveCount", "1");
    message.setAttributes(attributes);

    List<ConsumableEvent> result = typer.apply(List.of(message));

    assertThat(result).hasSize(1);
    ConsumableEvent consumableEvent = result.get(0);
    assertThat(consumableEvent.getEvent().typeName())
        .isEqualTo(TranscriptEmailRequested.class.getName());
    assertThat(consumableEvent.getEvent().payload()).isInstanceOf(TranscriptEmailRequested.class);
    assertThat(((TranscriptEmailRequested) consumableEvent.getEvent().payload()).getTranscriptId())
        .isEqualTo(event.getTranscriptId());
  }

  @Test
  void apply_skips_malformed_messages() {
    ObjectMapper om = new ObjectMapper();
    EventConf eventConf = mock(EventConf.class);
    ConsumableEventTyper typer = new ConsumableEventTyper(om, eventConf);

    SQSMessage message = new SQSMessage();
    message.setBody("not valid json");

    List<ConsumableEvent> result = typer.apply(List.of(message));

    assertThat(result).isEmpty();
  }
}
