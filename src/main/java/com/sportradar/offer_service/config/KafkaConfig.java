package com.sportradar.offer_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic marketTopic() {
        return TopicBuilder.name("market")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic eventTopic() {
        return TopicBuilder.name("event")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
