package com.stayfinder.booking.repository;

import com.stayfinder.booking.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelIdOrderByRoomNumberAsc(Long hotelId);

    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);

    boolean existsByHotelIdAndRoomNumberAndIdNot(Long hotelId, String roomNumber, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
