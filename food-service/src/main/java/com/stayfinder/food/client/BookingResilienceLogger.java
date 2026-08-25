package com.stayfinder.food.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BookingResilienceLogger {

    private static final Logger log = LoggerFactory.getLogger(BookingResilienceLogger.class);

    public BookingResilienceLogger(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        Retry retry = retryRegistry.retry("bookingService");
        retry.getEventPublisher().onRetry(event ->
                log.warn("Retrying Booking lookup attempt={} lastError={}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().toString()));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("bookingService");
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Booking circuit {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }
}
