package com.stayfinder.food.client;

import com.stayfinder.food.exception.BookingDependencyException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Wraps the Feign Booking call with Retry + CircuitBreaker.
 * Annotations must live on a Spring bean other than {@link BookingClient}:
 * Feign already creates a proxy, and a TimeLimiter thread-hop would drop the JWT relay.
 */
@Service
public class BookingLookupService {

    private static final Logger log = LoggerFactory.getLogger(BookingLookupService.class);

    private final BookingClient bookingClient;

    public BookingLookupService(BookingClient bookingClient) {
        this.bookingClient = bookingClient;
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookingUnavailable")
    @Retry(name = "bookingService")
    public BookingView getBooking(Long bookingId) {
        return bookingClient.getBooking(bookingId);
    }

    BookingView bookingUnavailable(Long bookingId, Throwable cause) {
        if (isClientError(cause) && cause instanceof RuntimeException runtime) {
            throw runtime;
        }
        log.warn("Booking lookup fallback bookingId={} cause={}", bookingId, cause.toString());
        throw new BookingDependencyException("Booking Service is unavailable");
    }

    private static boolean isClientError(Throwable cause) {
        return cause instanceof FeignException.NotFound
                || cause instanceof FeignException.Forbidden
                || cause instanceof FeignException.Unauthorized
                || cause instanceof FeignException.BadRequest;
    }
}
