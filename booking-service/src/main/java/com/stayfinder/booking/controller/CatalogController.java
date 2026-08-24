package com.stayfinder.booking.controller;

import com.stayfinder.booking.dto.HotelRequest;
import com.stayfinder.booking.dto.HotelResponse;
import com.stayfinder.booking.dto.RoomRequest;
import com.stayfinder.booking.dto.RoomResponse;
import com.stayfinder.booking.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/hotels")
    public List<HotelResponse> hotels() {
        return catalogService.listHotels();
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public List<RoomResponse> rooms(@PathVariable Long hotelId) {
        return catalogService.listRooms(hotelId);
    }

    @PostMapping("/hotels")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public HotelResponse createHotel(@Valid @RequestBody HotelRequest request) {
        return catalogService.createHotel(request);
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) {
        return catalogService.createRoom(request);
    }

    @PutMapping("/rooms/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
    public RoomResponse updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomRequest request) {
        return catalogService.updateRoom(roomId, request);
    }
}
