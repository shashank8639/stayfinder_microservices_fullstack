package com.stayfinder.food.dto;

import jakarta.validation.constraints.NotNull;

import com.stayfinder.food.entity.OrderStatus;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
