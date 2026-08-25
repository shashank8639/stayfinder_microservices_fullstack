package com.stayfinder.food.client;

public record BookingView(
        Long id,
        Long hotelId,
        String roomNumber,
        Long userId,
        String status
) {
}
