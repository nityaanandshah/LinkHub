package com.linkhub.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 * Uses the singleton container pattern so all test classes share
 * the same PostgreSQL, Redis and Kafka containers, avoiding port
 * mismatches when the Spring context is cached across test classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    // ── Singleton containers (started once, shared across ALL test classes) ──

    static final PostgreSQLContainer<?> postgres;
    static final GenericContainer<?> redis;
    static final KafkaContainer kafka;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("linkhub_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Flyway
        registry.add("spring.flyway.enabled", () -> true);
    }
}
