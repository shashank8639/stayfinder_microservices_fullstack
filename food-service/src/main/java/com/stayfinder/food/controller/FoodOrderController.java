package com.stayfinder.food.controller;

import com.stayfinder.food.dto.CreateFoodOrderRequest;
import com.stayfinder.food.dto.FoodOrderResponse;
import com.stayfinder.food.dto.UpdateOrderStatusRequest;
import com.stayfinder.food.security.CurrentUser;
import com.stayfinder.food.service.FoodOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    public FoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','HOTEL_OWNER')")
    public FoodOrderResponse create(
            @Valid @RequestBody CreateFoodOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return foodOrderService.create(request, CurrentUser.from(jwt));
    }

    @GetMapping("/orders/my")
    @PreAuthorize("isAuthenticated()")
    public List<FoodOrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return foodOrderService.myOrders(CurrentUser.from(jwt));
    }

    @GetMapping("/hotel/{hotelId}/orders")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public List<FoodOrderResponse> hotelOrders(@PathVariable Long hotelId) {
        return foodOrderService.hotelOrders(hotelId);
    }

    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public FoodOrderResponse updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return foodOrderService.updateStatus(orderId, request);
    }
}
