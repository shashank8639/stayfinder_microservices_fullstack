package com.stayfinder.food.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateFoodOrderRequest(
        @NotNull Long hotelId,
        @NotNull Long bookingId,
        String roomNumber,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
