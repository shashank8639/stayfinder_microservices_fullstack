package com.stayfinder.booking.repository;

import com.stayfinder.booking.entity.Booking;
import com.stayfinder.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.room r
            JOIN FETCH r.hotel
            WHERE b.userId = :userId
            ORDER BY b.checkIn DESC
            """)
    List<Booking> findByUserIdOrderByCheckInDesc(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.room r
            JOIN FETCH r.hotel
            WHERE r.hotel.id = :hotelId
            ORDER BY b.checkIn DESC
            """)
    List<Booking> findByRoomHotelIdOrderByCheckInDesc(@Param("hotelId") Long hotelId);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.room r
            JOIN FETCH r.hotel
            WHERE b.id = :id
            """)
    Optional<Booking> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status <> :cancelled
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
            """)
    long countOverlapping(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("cancelled") BookingStatus cancelled
    );
}
