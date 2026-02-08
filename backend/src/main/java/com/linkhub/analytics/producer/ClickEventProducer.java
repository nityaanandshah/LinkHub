package com.linkhub.analytics.producer;

import com.linkhub.analytics.dto.ClickEventMessage;
import com.linkhub.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ClickEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventProducer.class);

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    public ClickEventProducer(KafkaTemplate<String, ClickEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Fire-and-forget: publish a click event to Kafka.
     * Uses shortCode as the partition key for ordering guarantees per URL.
     * Failures are logged but never block the redirect response.
     */
    public void publishClickEvent(ClickEventMessage event) {
        try {
            CompletableFuture<SendResult<String, ClickEventMessage>> future =
                    kafkaTemplate.send(KafkaConfig.CLICK_EVENTS_TOPIC, event.shortCode(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish click event for shortCode={}: {}",
                            event.shortCode(), ex.getMessage());
                    // TODO (Week 3): Write to DLQ table for retry
                } else {
                    log.debug("Click event published for shortCode={}, partition={}, offset={}",
                            event.shortCode(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Kafka send threw exception for shortCode={}: {}",
                    event.shortCode(), e.getMessage());
            // Non-blocking — redirect must not fail because of analytics
        }
    }
}
