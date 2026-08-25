package com.stayfinder.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayfinder.food.client.BookingClient;
import com.stayfinder.food.client.BookingView;
import com.stayfinder.food.dto.CreateFoodOrderRequest;
import com.stayfinder.food.dto.MenuItemRequest;
import com.stayfinder.food.dto.OrderItemRequest;
import com.stayfinder.food.dto.UpdateOrderStatusRequest;
import com.stayfinder.food.entity.OrderStatus;
import com.stayfinder.food.event.FoodEventPublisher;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoodApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingClient bookingClient;

    @MockitoBean
    private FoodEventPublisher foodEventPublisher;

    @Test
    void publicMenuDoesNotRequireJwt() throws Exception {
        mockMvc.perform(get("/api/food/hotels/1/menu"))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanPlaceOrderWhenBookingIsConfirmed() throws Exception {
        long menuItemId = createMenuItem();
        when(bookingClient.getBooking(10L))
                .thenReturn(new BookingView(10L, 1L, "101", 42L, "CONFIRMED"));

        mockMvc.perform(post("/api/food/orders")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(menuItemId, 10L, "101"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.bookingId").value(10))
                .andExpect(jsonPath("$.roomNumber").value("101"));

        verify(foodEventPublisher).publishFoodOrderPlaced(argThat(event ->
                event.bookingId().equals(10L)
                        && event.userId().equals(42L)
                        && event.itemCount() == 1
                        && "101".equals(event.roomNumber())
        ));
    }

    @Test
    void orderIsRejectedWhenBookingIsNotConfirmed() throws Exception {
        long menuItemId = createMenuItem();
        when(bookingClient.getBooking(11L))
                .thenReturn(new BookingView(11L, 1L, "101", 42L, "PENDING"));

        mockMvc.perform(post("/api/food/orders")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(menuItemId, 11L, "101"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodEventPublisher);
    }

    @Test
    void orderIsRejectedWhenRoomNumberDoesNotMatch() throws Exception {
        long menuItemId = createMenuItem();
        when(bookingClient.getBooking(12L))
                .thenReturn(new BookingView(12L, 1L, "101", 42L, "CONFIRMED"));

        mockMvc.perform(post("/api/food/orders")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(menuItemId, 12L, "999"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/food/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(1L, 1L, "101"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotManageMenu() throws Exception {
        mockMvc.perform(post("/api/food/menu")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MenuItemRequest(1L, "Pizza", new BigDecimal("250.00"), true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void bookingServiceDownReturns503() throws Exception {
        long menuItemId = createMenuItem();
        Request feignRequest = Request.create(Request.HttpMethod.GET, "/api/bookings/13", Map.of(), null, StandardCharsets.UTF_8, null);
        when(bookingClient.getBooking(13L)).thenThrow(new FeignException.ServiceUnavailable("down", feignRequest, null, Map.of()));

        mockMvc.perform(post("/api/food/orders")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(menuItemId, 13L, "101"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        verify(bookingClient).getBooking(13L);
    }

    @Test
    void adminCanUpdateOrderStatus() throws Exception {
        long menuItemId = createMenuItem();
        when(bookingClient.getBooking(14L))
                .thenReturn(new BookingView(14L, 1L, "101", 42L, "CONFIRMED"));

        MvcResult created = mockMvc.perform(post("/api/food/orders")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest(menuItemId, 14L, "101"))))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/food/orders/" + orderId + "/status")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest(OrderStatus.PREPARING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
    }

    private long createMenuItem() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/food/menu")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MenuItemRequest(1L, "Biryani", new BigDecimal("400.00"), true))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private CreateFoodOrderRequest orderRequest(long menuItemId, long bookingId, String roomNumber) {
        return new CreateFoodOrderRequest(1L, bookingId, roomNumber, List.of(new OrderItemRequest(menuItemId, 1)));
    }

    private RequestPostProcessor customer(long userId) {
        return jwt().jwt(jwt -> jwt
                        .subject(String.valueOf(userId))
                        .claim("email", "customer" + userId + "@stayfinder.local")
                        .claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private RequestPostProcessor admin() {
        return jwt().jwt(jwt -> jwt
                        .subject("1")
                        .claim("email", "admin@stayfinder.local")
                        .claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
