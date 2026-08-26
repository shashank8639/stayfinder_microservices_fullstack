package com.stayfinder.notification.config;

import com.stayfinder.notification.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic bookingConfirmedTopic() {
        return TopicBuilder.name(KafkaTopics.BOOKING_CONFIRMED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic foodOrderPlacedTopic() {
        return TopicBuilder.name(KafkaTopics.FOOD_ORDER_PLACED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
