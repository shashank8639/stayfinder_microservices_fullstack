package com.stayfinder.booking.dto;

import java.math.BigDecimal;

public record RoomResponse(
        Long id,
        Long hotelId,
        String roomNumber,
        String roomType,
        BigDecimal pricePerNight
) {
}
