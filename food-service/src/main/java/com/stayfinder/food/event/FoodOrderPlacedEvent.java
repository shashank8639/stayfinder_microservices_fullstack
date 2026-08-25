package com.stayfinder.food.event;

import java.time.Instant;

/**
 * JSON payload published to Kafka. Not a JPA entity — Food's tables stay private.
 */
public record FoodOrderPlacedEvent(
        Long orderId,
        Long userId,
        String guestEmail,
        Long hotelId,
        Long bookingId,
        String roomNumber,
        int itemCount,
        Instant occurredAt
) {
}
