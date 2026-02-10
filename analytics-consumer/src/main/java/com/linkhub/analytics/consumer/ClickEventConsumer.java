package com.linkhub.analytics.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.analytics.config.KafkaConsumerConfig;
import com.linkhub.analytics.dto.ClickEventMessage;
import com.linkhub.analytics.model.FailedClickEvent;
import com.linkhub.analytics.repository.FailedClickEventRepository;
import com.linkhub.analytics.service.GeoIpService;
import com.linkhub.analytics.service.UserAgentParser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Kafka batch consumer for click events.
 *
 * <p>Processing pipeline per batch:
 * <ol>
 *   <li>Enrich each event (GeoIP + User-Agent parsing)</li>
 *   <li>Batch INSERT ... ON CONFLICT DO NOTHING into click_events</li>
 *   <li>Failed events → DLQ (Kafka topic + DB table)</li>
 * </ol>
 *
 * <p>Error handling strategy:
 * <ul>
 *   <li>Individual event failures don't fail the batch</li>
 *   <li>Enrichment failures result in null fields (graceful degradation)</li>
 *   <li>DB insert failures → publish to DLQ topic + write to DLQ table</li>
 * </ul>
 */
@Component
public class ClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    private static final String BATCH_INSERT_SQL = """
            INSERT INTO click_events (event_id, url_id, short_code, clicked_at,
                                      ip_address, user_agent, referrer,
                                      device_type, browser, os,
                                      country, city, latitude, longitude)
            VALUES (?, ?, ?, ?,
                    ?::inet, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?)
            ON CONFLICT (event_id, clicked_at) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GeoIpService geoIpService;
    private final UserAgentParser userAgentParser;
    private final FailedClickEventRepository failedClickEventRepository;
    private final KafkaTemplate<String, ClickEventMessage> dlqKafkaTemplate;
    private final ObjectMapper objectMapper;

    // Metrics
    private final Counter processedCounter;
    private final Counter failedCounter;
    private final Counter dlqCounter;
    private final Counter duplicateCounter;
    private final Timer batchProcessingTimer;
    private final Timer enrichmentTimer;

    public ClickEventConsumer(JdbcTemplate jdbcTemplate,
                              GeoIpService geoIpService,
                              UserAgentParser userAgentParser,
                              FailedClickEventRepository failedClickEventRepository,
                              KafkaTemplate<String, ClickEventMessage> dlqKafkaTemplate,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.geoIpService = geoIpService;
        this.userAgentParser = userAgentParser;
        this.failedClickEventRepository = failedClickEventRepository;
        this.dlqKafkaTemplate = dlqKafkaTemplate;
        this.objectMapper = objectMapper;

        this.processedCounter = Counter.builder("analytics.events.processed")
                .description("Click events successfully inserted")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("analytics.events.failed")
                .description("Click events that failed processing")
                .register(meterRegistry);
        this.dlqCounter = Counter.builder("analytics.events.dlq")
                .description("Click events sent to DLQ")
                .register(meterRegistry);
        this.duplicateCounter = Counter.builder("analytics.events.duplicates")
                .description("Duplicate click events skipped (ON CONFLICT)")
                .register(meterRegistry);
        this.batchProcessingTimer = Timer.builder("analytics.batch.processing.time")
                .description("Time to process a batch of click events")
                .register(meterRegistry);
        this.enrichmentTimer = Timer.builder("analytics.enrichment.time")
                .description("Time to enrich a single click event")
                .register(meterRegistry);
    }

    /**
     * Consume click events in batches from Kafka.
     * Enriches events, then does a JDBC batch insert for efficiency.
     */
    @KafkaListener(
            topics = "click-events",
            groupId = "analytics-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBatch(List<ClickEventMessage> events) {
        log.info("Received batch of {} click events", events.size());

        batchProcessingTimer.record(() -> {
            // 1. Enrich all events
            List<EnrichedEvent> enrichedEvents = new ArrayList<>(events.size());
            List<ClickEventMessage> failedEnrichment = new ArrayList<>();

            for (ClickEventMessage event : events) {
                try {
                    EnrichedEvent enriched = enrichmentTimer.record(() -> enrichEvent(event));
                    enrichedEvents.add(enriched);
                } catch (Exception e) {
                    log.error("Failed to enrich event eventId={}: {}", event.eventId(), e.getMessage());
                    failedEnrichment.add(event);
                    failedCounter.increment();
                }
            }

            // 2. Batch insert enriched events
            if (!enrichedEvents.isEmpty()) {
                try {
                    int[] results = batchInsert(enrichedEvents);
                    int inserted = 0;
                    int duplicates = 0;
                    for (int r : results) {
                        if (r > 0) inserted++;
                        else duplicates++;
                    }
                    processedCounter.increment(inserted);
                    duplicateCounter.increment(duplicates);
                    log.info("Batch insert complete: {} inserted, {} duplicates skipped",
                            inserted, duplicates);
                } catch (Exception e) {
                    log.error("Batch insert failed for {} events: {}", enrichedEvents.size(), e.getMessage());
                    // Fall back to individual inserts
                    for (EnrichedEvent enriched : enrichedEvents) {
                        try {
                            individualInsert(enriched);
                            processedCounter.increment();
                        } catch (Exception ex) {
                            log.error("Individual insert failed for eventId={}: {}",
                                    enriched.event.eventId(), ex.getMessage());
                            failedEnrichment.add(enriched.event);
                            failedCounter.increment();
                        }
                    }
                }
            }

            // 3. Send failed events to DLQ
            for (ClickEventMessage failed : failedEnrichment) {
                sendToDlq(failed, "Processing/insert failure");
            }
        });
    }

    // ────────── Enrichment ──────────

    private EnrichedEvent enrichEvent(ClickEventMessage event) {
        UserAgentParser.ParsedUserAgent ua = userAgentParser.parse(event.userAgent());

        Optional<GeoIpService.GeoLocation> geoOpt = geoIpService.resolve(event.ipAddress());

        return new EnrichedEvent(
                event,
                ua.deviceType(), ua.browser(), ua.os(),
                geoOpt.map(GeoIpService.GeoLocation::country).orElse(null),
                geoOpt.map(GeoIpService.GeoLocation::city).orElse(null),
                geoOpt.map(GeoIpService.GeoLocation::latitude).orElse(null),
                geoOpt.map(GeoIpService.GeoLocation::longitude).orElse(null)
        );
    }

    // ────────── Batch Insert ──────────

    private int[] batchInsert(List<EnrichedEvent> events) {
        return jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EnrichedEvent e = events.get(i);
                ps.setObject(1, e.event.eventId());
                ps.setLong(2, e.event.urlId());
                ps.setString(3, e.event.shortCode());
                ps.setTimestamp(4, Timestamp.from(e.event.clickedAt()));
                ps.setString(5, e.event.ipAddress());
                ps.setString(6, e.event.userAgent());
                ps.setString(7, e.event.referrer());
                ps.setString(8, e.deviceType);
                ps.setString(9, e.browser);
                ps.setString(10, e.os);
                setNullableString(ps, 11, e.country);
                setNullableString(ps, 12, e.city);
                setNullableDouble(ps, 13, e.latitude);
                setNullableDouble(ps, 14, e.longitude);
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });
    }

    private void individualInsert(EnrichedEvent e) {
        jdbcTemplate.update(BATCH_INSERT_SQL,
                e.event.eventId(), e.event.urlId(), e.event.shortCode(),
                Timestamp.from(e.event.clickedAt()),
                e.event.ipAddress(), e.event.userAgent(), e.event.referrer(),
                e.deviceType, e.browser, e.os,
                e.country, e.city, e.latitude, e.longitude);
    }

    // ────────── DLQ: Kafka Topic + DB Table ──────────

    private void sendToDlq(ClickEventMessage event, String reason) {
        dlqCounter.increment();

        // 1. Publish to Kafka DLQ topic
        try {
            dlqKafkaTemplate.send(KafkaConsumerConfig.CLICK_EVENTS_DLQ_TOPIC, event.shortCode(), event);
            log.info("Event published to DLQ topic: eventId={}", event.eventId());
        } catch (Exception e) {
            log.error("Failed to publish to DLQ topic for eventId={}: {}", event.eventId(), e.getMessage());
        }

        // 2. Write to DB DLQ table (backup)
        try {
            String payload = objectMapper.writeValueAsString(event);
            FailedClickEvent failed = new FailedClickEvent(event.eventId(), payload, reason);
            failedClickEventRepository.save(failed);
        } catch (Exception e) {
            log.error("Failed to write to DLQ table for eventId={}: {}", event.eventId(), e.getMessage());
        }
    }

    // ────────── Helpers ──────────

    private static void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value != null) ps.setString(idx, value);
        else ps.setNull(idx, Types.VARCHAR);
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, Double value) throws SQLException {
        if (value != null) ps.setDouble(idx, value);
        else ps.setNull(idx, Types.DOUBLE);
    }

    record EnrichedEvent(
            ClickEventMessage event,
            String deviceType, String browser, String os,
            String country, String city, Double latitude, Double longitude
    ) {}
}
