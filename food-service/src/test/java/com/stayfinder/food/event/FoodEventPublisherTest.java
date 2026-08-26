package com.stayfinder.food.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private FoodEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new FoodEventPublisher(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesJsonToFoodOrderPlacedTopic() throws Exception {
        FoodOrderPlacedEvent event = new FoodOrderPlacedEvent(
                9L,
                42L,
                "customer42@stayfinder.local",
                1L,
                10L,
                "101",
                2,
                Instant.parse("2026-08-14T10:05:00Z")
        );

        publisher.publishFoodOrderPlaced(event);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.FOOD_ORDER_PLACED),
                eq("9"),
                eq(objectMapper.writeValueAsString(event))
        );
    }
}
