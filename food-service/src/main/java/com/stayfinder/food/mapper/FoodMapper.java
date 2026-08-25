package com.stayfinder.food.mapper;

import com.stayfinder.food.dto.FoodOrderResponse;
import com.stayfinder.food.dto.MenuItemResponse;
import com.stayfinder.food.dto.OrderItemResponse;
import com.stayfinder.food.entity.FoodOrder;
import com.stayfinder.food.entity.MenuItem;

public final class FoodMapper {

    private FoodMapper() {
    }

    public static MenuItemResponse toMenuResponse(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getHotelId(), item.getName(), item.getPrice(), item.isAvailable());
    }

    public static FoodOrderResponse toOrderResponse(FoodOrder order) {
        return new FoodOrderResponse(
                order.getId(),
                order.getUserId(),
                order.getHotelId(),
                order.getBookingId(),
                order.getRoomNumber(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getMenuItemId(),
                                item.getItemName(),
                                item.getQuantity(),
                                item.getUnitPrice()
                        ))
                        .toList()
        );
    }
}
