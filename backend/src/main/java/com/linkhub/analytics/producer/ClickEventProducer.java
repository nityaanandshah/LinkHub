package com.linkhub.analytics.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.analytics.dto.ClickEventMessage;
import com.linkhub.analytics.model.FailedClickEvent;
import com.linkhub.analytics.repository.FailedClickEventRepository;
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
    private final FailedClickEventRepository failedClickEventRepository;
    private final ObjectMapper objectMapper;

    public ClickEventProducer(KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
                              FailedClickEventRepository failedClickEventRepository,
                              ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.failedClickEventRepository = failedClickEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Fire-and-forget: publish a click event to Kafka.
     * Uses shortCode as the partition key for ordering guarantees per URL.
     * On failure, writes to DLQ table for retry by DlqRetryJob.
     */
    public void publishClickEvent(ClickEventMessage event) {
        try {
            CompletableFuture<SendResult<String, ClickEventMessage>> future =
                    kafkaTemplate.send(KafkaConfig.CLICK_EVENTS_TOPIC, event.shortCode(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish click event for shortCode={}: {}",
                            event.shortCode(), ex.getMessage());
                    writeToDlq(event, ex.getMessage());
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
            writeToDlq(event, e.getMessage());
        }
    }

    /**
     * Write a failed event to the dead letter queue table.
     */
    private void writeToDlq(ClickEventMessage event, String reason) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            FailedClickEvent failed = new FailedClickEvent(event.eventId(), payload, reason);
            failedClickEventRepository.save(failed);
            log.info("Click event written to DLQ: eventId={}", event.eventId());
        } catch (Exception e) {
            log.error("Failed to write to DLQ for eventId={}: {}", event.eventId(), e.getMessage());
            // At this point, the event is lost. This is acceptable for analytics data.
        }
    }
}
