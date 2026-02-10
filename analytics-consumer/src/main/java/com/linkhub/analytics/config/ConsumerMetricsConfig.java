package com.linkhub.analytics.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for the analytics consumer.
 *
 * <p>Exposes:
 * <ul>
 *   <li>analytics.click_events.total — total rows in click_events (gauge)</li>
 *   <li>analytics.dlq.pending — pending DLQ items awaiting retry (gauge)</li>
 *   <li>analytics.dlq.exhausted — DLQ items that exceeded max retries (gauge)</li>
 *   <li>analytics.partitions.count — number of active click_events partitions (gauge)</li>
 * </ul>
 *
 * <p>These gauges are polled lazily by Prometheus at scrape time,
 * so the DB queries only run on the scrape interval (default 15s).
 */
@Component
public class ConsumerMetricsConfig implements MeterBinder {

    private final JdbcTemplate jdbcTemplate;

    public ConsumerMetricsConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Total click events (approximate, from pg_class for performance)
        Gauge.builder("analytics.click_events.total", this, ConsumerMetricsConfig::getClickEventCount)
                .description("Approximate total rows in click_events table")
                .register(registry);

        // Pending DLQ items (exact, small table)
        Gauge.builder("analytics.dlq.pending", this, ConsumerMetricsConfig::getPendingDlqCount)
                .description("Number of DLQ items pending retry")
                .register(registry);

        // Exhausted DLQ items (retry_count >= 5)
        Gauge.builder("analytics.dlq.exhausted", this, ConsumerMetricsConfig::getExhaustedDlqCount)
                .description("Number of DLQ items that exceeded max retries")
                .register(registry);

        // Active partition count
        Gauge.builder("analytics.partitions.count", this, ConsumerMetricsConfig::getPartitionCount)
                .description("Number of active click_events partitions")
                .register(registry);
    }

    private double getClickEventCount() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT reltuples::bigint FROM pg_class WHERE relname = 'click_events'",
                    Long.class);
            return count != null ? count.doubleValue() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private double getPendingDlqCount() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM failed_click_events WHERE retry_count < 5",
                    Long.class);
            return count != null ? count.doubleValue() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private double getExhaustedDlqCount() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM failed_click_events WHERE retry_count >= 5",
                    Long.class);
            return count != null ? count.doubleValue() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private double getPartitionCount() {
        try {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM pg_inherits
                    WHERE inhparent = 'click_events'::regclass
                    """, Long.class);
            return count != null ? count.doubleValue() : 0;
        } catch (Exception e) {
            return -1;
        }
    }
}
