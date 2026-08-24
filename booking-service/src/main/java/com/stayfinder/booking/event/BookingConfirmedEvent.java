package com.stayfinder.booking.event;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JSON payload published to Kafka. Not a JPA entity — Booking's tables stay private.
 */
public record BookingConfirmedEvent(
        Long bookingId,
        Long userId,
        String guestEmail,
        Long hotelId,
        String hotelName,
        String roomNumber,
        LocalDate checkIn,
        LocalDate checkOut,
        Instant occurredAt
) {
}
