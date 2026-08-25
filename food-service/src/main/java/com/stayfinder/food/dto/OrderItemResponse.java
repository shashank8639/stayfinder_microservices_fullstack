package com.stayfinder.food.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long menuItemId, String itemName, int quantity, BigDecimal unitPrice) {
}
