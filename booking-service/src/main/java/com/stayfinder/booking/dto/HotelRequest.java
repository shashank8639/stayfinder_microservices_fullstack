package com.stayfinder.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record HotelRequest(
        @NotBlank String name,
        @NotBlank String city,
        String description
) {
}
