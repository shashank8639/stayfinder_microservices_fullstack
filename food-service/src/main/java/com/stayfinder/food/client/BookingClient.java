package com.stayfinder.food.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", configuration = FeignConfig.class)
public interface BookingClient {

    @GetMapping("/api/bookings/{bookingId}")
    BookingView getBooking(@PathVariable("bookingId") Long bookingId);
}
