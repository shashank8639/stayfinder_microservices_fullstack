package com.stayfinder.food.client;

import feign.FeignException;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingResilienceConfig {

    /**
     * Inner Feign 4xx types are unreliable in YAML ({@code FeignException$NotFound}).
     * Ignoring them here so a missing booking does not retry or open the circuit.
     */
    @Bean
    @SuppressWarnings("unchecked")
    CircuitBreakerConfigCustomizer bookingServiceCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer.of("bookingService", builder -> builder
                .ignoreExceptions(
                        FeignException.NotFound.class,
                        FeignException.Forbidden.class,
                        FeignException.Unauthorized.class,
                        FeignException.BadRequest.class
                ));
    }

    @Bean
    @SuppressWarnings("unchecked")
    RetryConfigCustomizer bookingServiceRetryCustomizer() {
        return RetryConfigCustomizer.of("bookingService", builder -> builder
                .ignoreExceptions(
                        FeignException.NotFound.class,
                        FeignException.Forbidden.class,
                        FeignException.Unauthorized.class,
                        FeignException.BadRequest.class
                ));
    }
}
