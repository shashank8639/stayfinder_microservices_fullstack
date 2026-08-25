package com.stayfinder.food.dto;

import com.stayfinder.food.entity.OrderStatus;

import java.time.Instant;
import java.util.List;

public record FoodOrderResponse(
        Long id,
        Long userId,
        Long hotelId,
        Long bookingId,
        String roomNumber,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
