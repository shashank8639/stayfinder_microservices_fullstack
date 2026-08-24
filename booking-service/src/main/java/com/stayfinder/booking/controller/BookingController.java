package com.stayfinder.booking.controller;

import com.stayfinder.booking.dto.AvailabilityResponse;
import com.stayfinder.booking.dto.BookingResponse;
import com.stayfinder.booking.dto.CreateBookingRequest;
import com.stayfinder.booking.security.CurrentUser;
import com.stayfinder.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/rooms/{roomId}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return bookingService.checkAvailability(roomId, checkIn, checkOut);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','HOTEL_OWNER')")
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return bookingService.createBooking(request, CurrentUser.from(jwt));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.myBookings(CurrentUser.from(jwt));
    }

    @GetMapping("/hotel/{hotelId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public List<BookingResponse> hotelBookings(@PathVariable Long hotelId) {
        return bookingService.hotelBookings(hotelId);
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse getById(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.getById(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/{bookingId}/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','HOTEL_OWNER')")
    public BookingResponse confirm(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.confirm(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/{bookingId}/payment/confirm")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','HOTEL_OWNER')")
    public BookingResponse confirmPayment(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.confirmPayment(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','HOTEL_OWNER')")
    public BookingResponse cancel(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.cancel(bookingId, CurrentUser.from(jwt));
    }
}
