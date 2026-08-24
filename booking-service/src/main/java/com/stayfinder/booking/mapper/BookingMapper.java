package com.stayfinder.booking.mapper;

import com.stayfinder.booking.dto.BookingResponse;
import com.stayfinder.booking.dto.HotelResponse;
import com.stayfinder.booking.dto.RoomResponse;
import com.stayfinder.booking.entity.Booking;
import com.stayfinder.booking.entity.Hotel;
import com.stayfinder.booking.entity.Room;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static HotelResponse toHotelResponse(Hotel hotel) {
        return new HotelResponse(hotel.getId(), hotel.getName(), hotel.getCity(), hotel.getDescription());
    }

    public static RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getHotel().getId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getPricePerNight()
        );
    }

    public static BookingResponse toBookingResponse(Booking booking) {
        Room room = booking.getRoom();
        Hotel hotel = room.getHotel();
        return new BookingResponse(
                booking.getId(),
                hotel.getId(),
                hotel.getName(),
                room.getId(),
                room.getRoomNumber(),
                booking.getUserId(),
                booking.getGuestEmail(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
