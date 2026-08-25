package com.stayfinder.food.client;

import com.stayfinder.food.event.FoodEventPublisher;
import com.stayfinder.food.exception.BookingDependencyException;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "resilience4j.retry.instances.bookingService.max-attempts=3",
        "resilience4j.retry.instances.bookingService.wait-duration=1ms",
        "resilience4j.circuitbreaker.instances.bookingService.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.bookingService.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.bookingService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.bookingService.wait-duration-in-open-state=150ms",
        "resilience4j.circuitbreaker.instances.bookingService.permitted-number-of-calls-in-half-open-state=1"
})
class BookingLookupResilienceTest {

    @Autowired
    private BookingLookupService bookingLookupService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private BookingClient bookingClient;

    @MockitoBean
    private FoodEventPublisher foodEventPublisher;

    @BeforeEach
    void resetCircuit() {
        circuitBreakerRegistry.circuitBreaker("bookingService").reset();
        clearInvocations(bookingClient);
    }

    @Test
    void retriesTransientBookingFailuresThreeTimesThenFallsBack() {
        when(bookingClient.getBooking(13L)).thenThrow(unavailable(13L));

        assertThatThrownBy(() -> bookingLookupService.getBooking(13L))
                .isInstanceOf(BookingDependencyException.class)
                .hasMessageContaining("unavailable");

        verify(bookingClient, times(3)).getBooking(13L);
        assertThat(circuitBreaker("bookingService").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void doesNotRetryClientErrors() {
        when(bookingClient.getBooking(44L)).thenThrow(notFound(44L));

        assertThatThrownBy(() -> bookingLookupService.getBooking(44L))
                .isInstanceOf(FeignException.NotFound.class);

        verify(bookingClient, times(1)).getBooking(44L);
    }

    @Test
    void openCircuitRejectsWithoutCallingBooking() {
        when(bookingClient.getBooking(13L)).thenThrow(unavailable(13L));
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> bookingLookupService.getBooking(13L))
                    .isInstanceOf(BookingDependencyException.class);
        }
        assertThat(circuitBreaker("bookingService").getState()).isEqualTo(CircuitBreaker.State.OPEN);

        clearInvocations(bookingClient);
        assertThatThrownBy(() -> bookingLookupService.getBooking(13L))
                .isInstanceOf(BookingDependencyException.class);
        verifyNoMoreInteractions(bookingClient);
    }

    @Test
    void halfOpenProbeClosesCircuitOnSuccess() {
        when(bookingClient.getBooking(13L)).thenThrow(unavailable(13L));
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> bookingLookupService.getBooking(13L))
                    .isInstanceOf(BookingDependencyException.class);
        }
        assertThat(circuitBreaker("bookingService").getState()).isEqualTo(CircuitBreaker.State.OPEN);

        circuitBreaker("bookingService").transitionToHalfOpenState();
        reset(bookingClient);
        when(bookingClient.getBooking(13L)).thenReturn(new BookingView(13L, 1L, "101", 42L, "CONFIRMED"));

        BookingView view = bookingLookupService.getBooking(13L);
        assertThat(view.status()).isEqualTo("CONFIRMED");
        assertThat(circuitBreaker("bookingService").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private CircuitBreaker circuitBreaker(String name) {
        return circuitBreakerRegistry.circuitBreaker(name);
    }

    private static FeignException.ServiceUnavailable unavailable(long bookingId) {
        return new FeignException.ServiceUnavailable(
                "down",
                request(bookingId),
                null,
                Map.of()
        );
    }

    private static FeignException.NotFound notFound(long bookingId) {
        return new FeignException.NotFound(
                "missing",
                request(bookingId),
                null,
                Map.of()
        );
    }

    private static Request request(long bookingId) {
        return Request.create(
                Request.HttpMethod.GET,
                "/api/bookings/" + bookingId,
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
    }
}
