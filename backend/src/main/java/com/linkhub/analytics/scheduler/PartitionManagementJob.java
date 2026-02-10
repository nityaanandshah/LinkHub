package com.linkhub.analytics.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Automated partition management for the click_events partitioned table.
 *
 * <p>Strategy:
 * <ul>
 *   <li>Runs daily at midnight UTC</li>
 *   <li>Creates partitions 3 months ahead (ensures inserts never fail due to missing partition)</li>
 *   <li>Detaches partitions older than the configured retention period (default 12 months)</li>
 *   <li>Idempotent — safe to run multiple times (IF NOT EXISTS semantics via exception handling)</li>
 * </ul>
 */
@Component
public class PartitionManagementJob {

    private static final Logger log = LoggerFactory.getLogger(PartitionManagementJob.class);

    private static final int MONTHS_AHEAD = 3;
    private static final int RETENTION_MONTHS = 12;
    private static final DateTimeFormatter PARTITION_SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");

    private final JdbcTemplate jdbcTemplate;

    public PartitionManagementJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Create future partitions and detach old ones.
     * Runs daily at 00:05 UTC.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void managePartitions() {
        log.info("Partition management job started");
        createFuturePartitions();
        detachOldPartitions();
        log.info("Partition management job completed");
    }

    /**
     * Creates monthly partitions for the next N months.
     * Uses try-catch per partition so one failure doesn't block others.
     */
    private void createFuturePartitions() {
        LocalDate now = LocalDate.now();

        for (int i = 0; i <= MONTHS_AHEAD; i++) {
            LocalDate month = now.plusMonths(i).withDayOfMonth(1);
            LocalDate nextMonth = month.plusMonths(1);

            String partitionName = "click_events_" + month.format(PARTITION_SUFFIX);
            String fromDate = month.toString();
            String toDate = nextMonth.toString();

            try {
                // Check if partition already exists
                List<String> existing = jdbcTemplate.queryForList(
                        "SELECT tablename FROM pg_tables WHERE tablename = ?",
                        String.class, partitionName);

                if (!existing.isEmpty()) {
                    log.debug("Partition {} already exists, skipping", partitionName);
                    continue;
                }

                String sql = String.format(
                        "CREATE TABLE %s PARTITION OF click_events FOR VALUES FROM ('%s') TO ('%s')",
                        partitionName, fromDate, toDate);

                jdbcTemplate.execute(sql);
                log.info("Created partition: {} [{} to {})", partitionName, fromDate, toDate);
            } catch (Exception e) {
                // Partition may already exist (race condition) — safe to ignore
                log.debug("Partition {} creation skipped: {}", partitionName, e.getMessage());
            }
        }
    }

    /**
     * Detach partitions older than the retention period.
     * Detached partitions remain as standalone tables for archival/deletion.
     */
    private void detachOldPartitions() {
        LocalDate cutoff = LocalDate.now().minusMonths(RETENTION_MONTHS).withDayOfMonth(1);

        try {
            // Find all child partitions of click_events
            List<String> partitions = jdbcTemplate.queryForList("""
                    SELECT inhrelid::regclass::text AS partition_name
                    FROM pg_inherits
                    WHERE inhparent = 'click_events'::regclass
                    ORDER BY partition_name
                    """, String.class);

            for (String partition : partitions) {
                // Extract date from partition name (e.g., "click_events_2025_01" → 2025-01)
                String datePart = partition.replace("click_events_", "");
                try {
                    LocalDate partitionDate = LocalDate.parse(
                            datePart.replace("_", "-") + "-01");

                    if (partitionDate.isBefore(cutoff)) {
                        String sql = String.format(
                                "ALTER TABLE click_events DETACH PARTITION %s", partition);
                        jdbcTemplate.execute(sql);
                        log.info("Detached old partition: {} (older than {} months)", partition, RETENTION_MONTHS);
                    }
                } catch (Exception e) {
                    log.debug("Skipping partition {} for detach check: {}", partition, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during partition detachment: {}", e.getMessage());
        }
    }

    /**
     * Get the list of current partitions (useful for health checks and monitoring).
     */
    public List<String> listPartitions() {
        return jdbcTemplate.queryForList("""
                SELECT inhrelid::regclass::text AS partition_name
                FROM pg_inherits
                WHERE inhparent = 'click_events'::regclass
                ORDER BY partition_name
                """, String.class);
    }

    /**
     * Get partition stats: name, row count estimate, and size.
     */
    public List<PartitionInfo> getPartitionStats() {
        return jdbcTemplate.query("""
                SELECT
                    inhrelid::regclass::text AS partition_name,
                    pg_relation_size(inhrelid) AS size_bytes,
                    (SELECT reltuples::bigint FROM pg_class WHERE oid = inhrelid) AS row_estimate
                FROM pg_inherits
                WHERE inhparent = 'click_events'::regclass
                ORDER BY partition_name
                """, (rs, rowNum) -> new PartitionInfo(
                rs.getString("partition_name"),
                rs.getLong("size_bytes"),
                rs.getLong("row_estimate")
        ));
    }

    public record PartitionInfo(String name, long sizeBytes, long rowEstimate) {}
}
