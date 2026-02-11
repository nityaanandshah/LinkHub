package com.linkhub.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Exposes Kafka consumer lag information for the analytics pipeline.
 * Used by the frontend to show a "data may be delayed" indicator.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System health and info endpoints")
public class HealthInfoController {

    private static final Logger log = LoggerFactory.getLogger(HealthInfoController.class);
    private static final String CONSUMER_GROUP = "analytics-consumer-group";
    private static final long LAG_THRESHOLD = 1000; // messages behind → show warning

    private final KafkaAdmin kafkaAdmin;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public HealthInfoController(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @GetMapping("/analytics-lag")
    @Operation(summary = "Analytics consumer lag",
            description = "Returns consumer group lag and whether analytics data may be delayed")
    public ResponseEntity<AnalyticsLagResponse> getAnalyticsLag() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListConsumerGroupOffsetsResult offsetsResult =
                    adminClient.listConsumerGroupOffsets(CONSUMER_GROUP);

            Map<TopicPartition, OffsetAndMetadata> offsets =
                    offsetsResult.partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);

            if (offsets == null || offsets.isEmpty()) {
                return ResponseEntity.ok(new AnalyticsLagResponse(0, false, "Consumer group not active"));
            }

            // Get end offsets for all partitions the consumer is assigned to
            Map<TopicPartition, Long> endOffsets = adminClient
                    .listOffsets(offsets.entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> org.apache.kafka.clients.admin.OffsetSpec.latest())))
                    .all().get(5, TimeUnit.SECONDS)
                    .entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().offset()));

            long totalLag = 0;
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                Long endOffset = endOffsets.get(entry.getKey());
                if (endOffset != null) {
                    totalLag += Math.max(0, endOffset - entry.getValue().offset());
                }
            }

            boolean isDelayed = totalLag > LAG_THRESHOLD;
            String message = isDelayed
                    ? "Analytics data may be delayed (" + totalLag + " events behind)"
                    : "Analytics data is up to date";

            return ResponseEntity.ok(new AnalyticsLagResponse(totalLag, isDelayed, message));

        } catch (Exception e) {
            log.warn("Failed to check analytics consumer lag: {}", e.getMessage());
            // If we can't check, assume it's fine rather than alarming the user
            return ResponseEntity.ok(new AnalyticsLagResponse(-1, false, "Unable to check consumer lag"));
        }
    }

    public record AnalyticsLagResponse(long lag, boolean delayed, String message) {}
}
