package com.linkhub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CLICK_EVENTS_TOPIC = "click-events";
    public static final String CLICK_EVENTS_DLQ_TOPIC = "click-events-dlq";

    @Bean
    public NewTopic clickEventsTopic() {
        return TopicBuilder.name(CLICK_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic clickEventsDlqTopic() {
        return TopicBuilder.name(CLICK_EVENTS_DLQ_TOPIC)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000)) // 7 days
                .build();
    }
}
