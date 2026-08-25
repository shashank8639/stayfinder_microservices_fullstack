package com.stayfinder.food.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotNull Long hotelId,
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        Boolean available
) {
}
