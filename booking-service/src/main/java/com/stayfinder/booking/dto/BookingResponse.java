package com.stayfinder.booking.dto;

import com.stayfinder.booking.entity.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long hotelId,
        String hotelName,
        Long roomId,
        String roomNumber,
        Long userId,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        BookingStatus status,
        Instant createdAt
) {
}
