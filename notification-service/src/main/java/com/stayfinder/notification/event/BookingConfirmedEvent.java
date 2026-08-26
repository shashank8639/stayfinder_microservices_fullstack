package com.stayfinder.notification.event;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Notification's copy of the booking-confirmed payload.
 * Duplicated on purpose — Booking does not share a JAR of events.
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
