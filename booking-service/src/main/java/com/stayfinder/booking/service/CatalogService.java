package com.stayfinder.booking.service;

import com.stayfinder.booking.dto.HotelRequest;
import com.stayfinder.booking.dto.HotelResponse;
import com.stayfinder.booking.dto.RoomRequest;
import com.stayfinder.booking.dto.RoomResponse;
import com.stayfinder.booking.entity.Hotel;
import com.stayfinder.booking.entity.Room;
import com.stayfinder.booking.exception.BookingConflictException;
import com.stayfinder.booking.exception.ResourceNotFoundException;
import com.stayfinder.booking.mapper.BookingMapper;
import com.stayfinder.booking.repository.HotelRepository;
import com.stayfinder.booking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public CatalogService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> listHotels() {
        return hotelRepository.findAll().stream().map(BookingMapper::toHotelResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(Long hotelId) {
        requireHotel(hotelId);
        return roomRepository.findByHotelIdOrderByRoomNumberAsc(hotelId).stream()
                .map(BookingMapper::toRoomResponse)
                .toList();
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.name().trim());
        hotel.setCity(request.city().trim());
        hotel.setDescription(request.description());
        hotelRepository.save(hotel);
        log.info("Hotel created id={} city={}", hotel.getId(), hotel.getCity());
        return BookingMapper.toHotelResponse(hotel);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Hotel hotel = requireHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndRoomNumber(hotel.getId(), request.roomNumber().trim())) {
            throw new BookingConflictException("Room number already exists in this hotel");
        }
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(request.roomNumber().trim());
        room.setRoomType(request.roomType().trim());
        room.setPricePerNight(request.pricePerNight());
        roomRepository.save(room);
        log.info("Room created id={} hotelId={}", room.getId(), hotel.getId());
        return BookingMapper.toRoomResponse(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        Hotel hotel = requireHotel(request.hotelId());
        if (roomRepository.existsByHotelIdAndRoomNumberAndIdNot(hotel.getId(), request.roomNumber().trim(), roomId)) {
            throw new BookingConflictException("Room number already exists in this hotel");
        }
        room.setHotel(hotel);
        room.setRoomNumber(request.roomNumber().trim());
        room.setRoomType(request.roomType().trim());
        room.setPricePerNight(request.pricePerNight());
        log.info("Room updated id={}", roomId);
        return BookingMapper.toRoomResponse(room);
    }

    private Hotel requireHotel(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));
    }
}
