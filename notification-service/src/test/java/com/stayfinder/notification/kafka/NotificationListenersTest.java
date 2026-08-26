package com.stayfinder.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayfinder.notification.event.BookingConfirmedEvent;
import com.stayfinder.notification.event.FoodOrderPlacedEvent;
import com.stayfinder.notification.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.BOOKING_CONFIRMED, KafkaTopics.FOOD_ORDER_PLACED},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class NotificationListenersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private EmailNotificationService emailNotificationService;

    @Test
    void healthIsPublicAndBusinessApiIsDenied() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications")).andExpect(status().isForbidden());
    }

    @Test
    void consumesBookingConfirmedAndLogsEmail() throws Exception {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                10L,
                42L,
                "customer@stayfinder.local",
                1L,
                "Grand Horizon Hyderabad",
                "101",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12),
                Instant.parse("2026-08-14T12:00:00Z")
        );

        kafkaTemplate.send(KafkaTopics.BOOKING_CONFIRMED, "10", objectMapper.writeValueAsString(event))
                .get(5, TimeUnit.SECONDS);

        verify(emailNotificationService, timeout(10_000)).notifyBookingConfirmed(argThat(received ->
                received.bookingId().equals(10L)
                        && "customer@stayfinder.local".equals(received.guestEmail())
                        && "Grand Horizon Hyderabad".equals(received.hotelName())
        ));
    }

    @Test
    void consumesFoodOrderPlacedAndLogsEmail() throws Exception {
        FoodOrderPlacedEvent event = new FoodOrderPlacedEvent(
                7L,
                42L,
                "customer@stayfinder.local",
                1L,
                10L,
                "101",
                2,
                Instant.parse("2026-08-14T12:05:00Z")
        );

        kafkaTemplate.send(KafkaTopics.FOOD_ORDER_PLACED, "7", objectMapper.writeValueAsString(event))
                .get(5, TimeUnit.SECONDS);

        verify(emailNotificationService, timeout(10_000)).notifyFoodOrderPlaced(argThat(received ->
                received.orderId().equals(7L)
                        && received.bookingId().equals(10L)
                        && received.itemCount() == 2
        ));
    }
}
