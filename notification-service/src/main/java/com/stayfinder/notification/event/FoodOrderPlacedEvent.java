package com.stayfinder.notification.event;

import java.time.Instant;

/**
 * Notification's copy of the food-order-placed payload.
 * Duplicated on purpose — Food does not share a JAR of events.
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
