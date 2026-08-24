package com.stayfinder.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RoomRequest(
        @NotNull Long hotelId,
        @NotBlank String roomNumber,
        @NotBlank String roomType,
        @NotNull @DecimalMin("0.0") BigDecimal pricePerNight
) {
}
