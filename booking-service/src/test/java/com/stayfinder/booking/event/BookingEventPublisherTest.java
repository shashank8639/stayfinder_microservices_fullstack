package com.stayfinder.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private BookingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new BookingEventPublisher(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesJsonToBookingConfirmedTopic() throws Exception {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                5L,
                21L,
                "customer21@stayfinder.local",
                1L,
                "Grand Horizon Hyderabad",
                "101",
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 22),
                Instant.parse("2026-08-14T10:00:00Z")
        );

        publisher.publishBookingConfirmed(event);

        verify(kafkaTemplate).send(
                eq(KafkaTopics.BOOKING_CONFIRMED),
                eq("5"),
                eq(objectMapper.writeValueAsString(event))
        );
    }
}
