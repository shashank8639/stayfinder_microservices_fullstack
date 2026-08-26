package com.stayfinder.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayfinder.notification.event.BookingConfirmedEvent;
import com.stayfinder.notification.event.FoodOrderPlacedEvent;
import com.stayfinder.notification.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListeners {

    private static final Logger log = LoggerFactory.getLogger(NotificationListeners.class);

    private final ObjectMapper objectMapper;
    private final EmailNotificationService emailNotificationService;

    public NotificationListeners(ObjectMapper objectMapper, EmailNotificationService emailNotificationService) {
        this.objectMapper = objectMapper;
        this.emailNotificationService = emailNotificationService;
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CONFIRMED, groupId = "notification-service")
    public void onBookingConfirmed(String payload) {
        try {
            BookingConfirmedEvent event = objectMapper.readValue(payload, BookingConfirmedEvent.class);
            emailNotificationService.notifyBookingConfirmed(event);
        } catch (Exception ex) {
            log.error("Failed to process BookingConfirmed payload={}", payload, ex);
        }
    }

    @KafkaListener(topics = KafkaTopics.FOOD_ORDER_PLACED, groupId = "notification-service")
    public void onFoodOrderPlaced(String payload) {
        try {
            FoodOrderPlacedEvent event = objectMapper.readValue(payload, FoodOrderPlacedEvent.class);
            emailNotificationService.notifyFoodOrderPlaced(event);
        } catch (Exception ex) {
            log.error("Failed to process FoodOrderPlaced payload={}", payload, ex);
        }
    }
}
