package com.stayfinder.food.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class FoodEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FoodEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FoodEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishFoodOrderPlaced(FoodOrderPlacedEvent event) {
        runAfterCommit(() -> send(KafkaTopics.FOOD_ORDER_PLACED, String.valueOf(event.orderId()), event));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void send(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} key={}", topic, key, ex);
                } else {
                    log.info("Published {} key={}", topic, key);
                }
            });
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize {} key={}", topic, key, ex);
        }
    }
}
