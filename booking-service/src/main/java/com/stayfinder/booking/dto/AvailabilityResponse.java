package com.stayfinder.booking.dto;

public record AvailabilityResponse(Long roomId, boolean available, String message) {
}
