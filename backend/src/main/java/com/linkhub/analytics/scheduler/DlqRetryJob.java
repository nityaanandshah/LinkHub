package com.linkhub.analytics.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.analytics.dto.ClickEventMessage;
import com.linkhub.analytics.model.FailedClickEvent;
import com.linkhub.analytics.repository.FailedClickEventRepository;
import com.linkhub.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job that retries failed click events from the DLQ table.
 * Runs every 2 minutes, picks up events with retry_count < 5 and next_retry_at <= now,
 * and re-publishes them to Kafka.
 */
@Component
public class DlqRetryJob {

    private static final Logger log = LoggerFactory.getLogger(DlqRetryJob.class);
    private static final int MAX_RETRIES = 5;

    private final FailedClickEventRepository failedClickEventRepository;
    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DlqRetryJob(FailedClickEventRepository failedClickEventRepository,
                       KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
                       ObjectMapper objectMapper) {
        this.failedClickEventRepository = failedClickEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 120_000) // Every 2 minutes
    public void retryFailedEvents() {
        List<FailedClickEvent> retryable = failedClickEventRepository
                .findRetryableEvents(MAX_RETRIES, Instant.now());

        if (retryable.isEmpty()) {
            return;
        }

        log.info("DLQ retry job: found {} events to retry", retryable.size());

        for (FailedClickEvent failed : retryable) {
            try {
                ClickEventMessage event = objectMapper.readValue(failed.getPayload(), ClickEventMessage.class);

                kafkaTemplate.send(KafkaConfig.CLICK_EVENTS_TOPIC, event.shortCode(), event).get();

                // Success — remove from DLQ
                failedClickEventRepository.delete(failed);
                log.info("DLQ retry succeeded for eventId={}", failed.getEventId());

            } catch (Exception e) {
                log.warn("DLQ retry failed for eventId={}, attempt {}: {}",
                        failed.getEventId(), failed.getRetryCount() + 1, e.getMessage());

                failed.scheduleRetry();
                failed.setFailureReason(e.getMessage());
                failedClickEventRepository.save(failed);
            }
        }
    }
}
