package com.stayfinder.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull Long roomId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        String guestEmail
) {
    public CreateBookingRequest(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        this(roomId, checkIn, checkOut, null);
    }
}
