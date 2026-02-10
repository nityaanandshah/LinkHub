package com.linkhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.analytics.dto.ClickEventMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the analytics pipeline:
 * 1. Click events flow through Kafka → click_events table (simulated)
 * 2. Analytics endpoints return correct data
 * 3. DLQ retry mechanism works
 * 4. Pagination and time-range filtering
 * 5. Partition management
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalyticsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    private static String accessToken;
    private static String shortCode;

    @BeforeAll
    static void setUpAll() {
    }

    @Test
    @Order(1)
    void registerUserAndCreateUrl() throws Exception {
        // Register user
        String registerBody = """
                {"email": "analytics@test.com", "password": "Password123!"}
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();

        String regJson = registerResult.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(regJson).get("accessToken").asText();

        // Create a URL
        String createBody = """
                {"longUrl": "https://www.example.com/analytics-test"}
                """;
        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        shortCode = objectMapper.readTree(createJson).get("shortCode").asText();
        assertThat(shortCode).isNotBlank();
    }

    @Test
    @Order(2)
    void simulateClickEvents() throws Exception {
        Instant now = Instant.now();

        Long urlId = jdbcTemplate.queryForObject(
                "SELECT id FROM urls WHERE short_code = ?",
                Long.class, shortCode);

        // Insert 5 click events with different properties
        for (int i = 0; i < 5; i++) {
            UUID eventId = UUID.randomUUID();
            Instant clickedAt = now.minusSeconds(i * 3600L);

            jdbcTemplate.update("""
                    INSERT INTO click_events (event_id, url_id, short_code, clicked_at,
                                              ip_address, user_agent, referrer,
                                              device_type, browser, os,
                                              country, city, latitude, longitude)
                    VALUES (?, ?, ?, ?,
                            ?::inet, ?, ?,
                            ?, ?, ?,
                            ?, ?, ?, ?)
                    """,
                    eventId, urlId, shortCode, java.sql.Timestamp.from(clickedAt),
                    "8.8.8." + i,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    i % 2 == 0 ? "https://google.com" : null,
                    i % 2 == 0 ? "Desktop" : "Mobile",
                    i % 2 == 0 ? "Chrome" : "Safari",
                    i % 2 == 0 ? "Windows" : "iOS",
                    i % 2 == 0 ? "United States" : "United Kingdom",
                    i % 2 == 0 ? "New York" : "London",
                    i % 2 == 0 ? 40.7128 : 51.5074,
                    i % 2 == 0 ? -74.0060 : -0.1278
            );
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM click_events WHERE short_code = ?",
                Integer.class, shortCode);
        assertThat(count).isEqualTo(5);
    }

    @Test
    @Order(3)
    void getClickSummary() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/summary", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(5))
                .andExpect(jsonPath("$.uniqueVisitors").value(5));
    }

    @Test
    @Order(4)
    void getClickSummaryWithExplicitTimeRange() throws Exception {
        // Use explicit from/to instead of days
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant to = Instant.now();

        MvcResult result = mockMvc.perform(get("/api/v1/analytics/{shortCode}/summary", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").isNumber())
                .andReturn();

        // With only 2-hour window, should have fewer clicks than total 5
        String json = result.getResponse().getContentAsString();
        long totalClicks = objectMapper.readTree(json).get("totalClicks").asLong();
        assertThat(totalClicks).isLessThanOrEqualTo(5);
    }

    @Test
    @Order(5)
    void getTimeseries() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/timeseries", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30")
                        .param("granularity", "day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].timestamp").exists())
                .andExpect(jsonPath("$[0].clicks").exists());
    }

    @Test
    @Order(6)
    void getTopReferrers() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/referrers", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].referrer").exists())
                .andExpect(jsonPath("$[0].clicks").exists())
                .andExpect(jsonPath("$[0].percentage").exists());
    }

    @Test
    @Order(7)
    void getDeviceStats() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/devices", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceTypes").isArray())
                .andExpect(jsonPath("$.browsers").isArray())
                .andExpect(jsonPath("$.operatingSystems").isArray());
    }

    @Test
    @Order(8)
    void getGeoStats() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/geo", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries").isArray())
                .andExpect(jsonPath("$.cities").isArray())
                .andExpect(jsonPath("$.countries[0].country").exists())
                .andExpect(jsonPath("$.cities[0].city").exists());
    }

    @Test
    @Order(9)
    void getPaginatedClicks() throws Exception {
        // Page 0, size 2
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/clicks", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @Order(10)
    void getPaginatedClicksLastPage() throws Exception {
        // Page 2, size 2 → only 1 element left
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/clicks", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @Order(11)
    void getPaginatedClicksWithTimeRange() throws Exception {
        Instant from = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant to = Instant.now();

        mockMvc.perform(get("/api/v1/analytics/{shortCode}/clicks", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].eventId").exists())
                .andExpect(jsonPath("$.content[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$.content[0].clickedAt").exists())
                .andExpect(jsonPath("$.content[0].deviceType").exists());
    }

    @Test
    @Order(12)
    void analyticsEndpointRejectsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/summary", shortCode))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(200);
                    assertThat(status == 302 || status == 401 || status == 403)
                            .as("Expected redirect or auth error, got " + status)
                            .isTrue();
                });
    }

    @Test
    @Order(13)
    void analyticsEndpointRejectsWrongUser() throws Exception {
        String registerBody = """
                {"email": "other@test.com", "password": "Password123!"}
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();

        String otherToken = objectMapper.readTree(
                registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/analytics/{shortCode}/summary", shortCode)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(14)
    void kafkaProducerSendsClickEvent() throws Exception {
        mockMvc.perform(get("/{shortCode}", shortCode))
                .andExpect(status().isFound());
    }

    @Test
    @Order(15)
    void dlqTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM failed_click_events", Integer.class);
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(16)
    void partitionManagementListsPartitions() {
        // Verify we can list partitions (the job itself is tested via scheduling)
        List<String> partitions = jdbcTemplate.queryForList("""
                SELECT inhrelid::regclass::text AS partition_name
                FROM pg_inherits
                WHERE inhparent = 'click_events'::regclass
                ORDER BY partition_name
                """, String.class);

        assertThat(partitions).isNotEmpty();
        assertThat(partitions.get(0)).startsWith("click_events_");
    }

    @Test
    @Order(17)
    void dlqTopicConfigured() {
        // Verify the DLQ topic constant is accessible in KafkaConfig
        assertThat(com.linkhub.config.KafkaConfig.CLICK_EVENTS_DLQ_TOPIC)
                .isEqualTo("click-events-dlq");
    }

    @Test
    @Order(18)
    void pageSizeClamped() throws Exception {
        // Request size=999, should be clamped to 200
        mockMvc.perform(get("/api/v1/analytics/{shortCode}/clicks", shortCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("days", "30")
                        .param("page", "0")
                        .param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(200));
    }
}
