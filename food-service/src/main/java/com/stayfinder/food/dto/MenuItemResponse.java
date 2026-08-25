package com.stayfinder.food.dto;

import java.math.BigDecimal;

public record MenuItemResponse(Long id, Long hotelId, String name, BigDecimal price, boolean available) {
}
