package com.stayfinder.notification.service;

import com.stayfinder.notification.event.BookingConfirmedEvent;
import com.stayfinder.notification.event.FoodOrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    public void notifyBookingConfirmed(BookingConfirmedEvent event) {
        log.info(
                "Email notification sent to customer {} for confirmed booking {} at {} (room {})",
                event.guestEmail(),
                event.bookingId(),
                event.hotelName(),
                event.roomNumber()
        );
    }

    public void notifyFoodOrderPlaced(FoodOrderPlacedEvent event) {
        log.info(
                "Email notification sent to customer {} for food order {} (booking {}, room {})",
                event.guestEmail(),
                event.orderId(),
                event.bookingId(),
                event.roomNumber()
        );
    }
}
