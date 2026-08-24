package com.stayfinder.booking.service;

import com.stayfinder.booking.dto.AvailabilityResponse;
import com.stayfinder.booking.dto.BookingResponse;
import com.stayfinder.booking.dto.CreateBookingRequest;
import com.stayfinder.booking.entity.Booking;
import com.stayfinder.booking.entity.BookingStatus;
import com.stayfinder.booking.entity.Room;
import com.stayfinder.booking.event.BookingConfirmedEvent;
import com.stayfinder.booking.event.BookingEventPublisher;
import com.stayfinder.booking.exception.BookingConflictException;
import com.stayfinder.booking.exception.InvalidBookingException;
import com.stayfinder.booking.exception.ResourceNotFoundException;
import com.stayfinder.booking.mapper.BookingMapper;
import com.stayfinder.booking.repository.BookingRepository;
import com.stayfinder.booking.repository.HotelRepository;
import com.stayfinder.booking.repository.RoomRepository;
import com.stayfinder.booking.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            BookingEventPublisher bookingEventPublisher
    ) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.bookingEventPublisher = bookingEventPublisher;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        requireRoom(roomId);
        validateDates(checkIn, checkOut);
        boolean available = !hasOverlap(roomId, checkIn, checkOut);
        return new AvailabilityResponse(
                roomId,
                available,
                available ? "Room is available" : "Room has an overlapping booking"
        );
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, CurrentUser currentUser) {
        validateDates(request.checkIn(), request.checkOut());
        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.roomId()));

        if (hasOverlap(room.getId(), request.checkIn(), request.checkOut())) {
            throw new BookingConflictException("Room is not available for the selected dates");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUserId(currentUser.userId());
        booking.setGuestEmail(resolveGuestEmail(request, currentUser));
        booking.setCheckIn(request.checkIn());
        booking.setCheckOut(request.checkOut());
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        log.info("Booking created id={} roomId={} userId={}", booking.getId(), room.getId(), currentUser.userId());
        return BookingMapper.toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(CurrentUser currentUser) {
        return bookingRepository.findByUserIdOrderByCheckInDesc(currentUser.userId()).stream()
                .map(BookingMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> hotelBookings(Long hotelId) {
        hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));
        return bookingRepository.findByRoomHotelIdOrderByCheckInDesc(hotelId).stream()
                .map(BookingMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long bookingId, CurrentUser currentUser) {
        return BookingMapper.toBookingResponse(requireOwnedOrAdmin(bookingId, currentUser));
    }

    @Transactional
    public BookingResponse confirm(Long bookingId, CurrentUser currentUser) {
        Booking booking = markConfirmed(bookingId, currentUser);
        log.info("Booking confirmed id={}", bookingId);
        return BookingMapper.toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse confirmPayment(Long bookingId, CurrentUser currentUser) {
        Booking booking = markConfirmed(bookingId, currentUser);
        log.info("Mock payment confirmed for booking id={}", bookingId);
        return BookingMapper.toBookingResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, CurrentUser currentUser) {
        Booking booking = requireOwnedOrAdmin(bookingId, currentUser);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException("Booking is already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        log.info("Booking cancelled id={}", bookingId);
        return BookingMapper.toBookingResponse(booking);
    }

    private boolean hasOverlap(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingRepository.countOverlapping(roomId, checkIn, checkOut, BookingStatus.CANCELLED) > 0;
    }

    private String resolveGuestEmail(CreateBookingRequest request, CurrentUser currentUser) {
        String provided = request.guestEmail() == null ? "" : request.guestEmail().trim();
        if (currentUser.isStaff()) {
            if (provided.isEmpty() || !provided.contains("@")) {
                throw new InvalidBookingException("Enter the guest's email to book at the front desk");
            }
            return provided;
        }
        if (currentUser.email() == null || currentUser.email().isBlank()) {
            return "unknown@stayfinder.local";
        }
        return currentUser.email();
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new InvalidBookingException("checkOut must be after checkIn");
        }
    }

    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
    }

    private Booking requireOwnedOrAdmin(Long bookingId, CurrentUser currentUser) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!currentUser.isStaff() && !booking.getUserId().equals(currentUser.userId())) {
            throw new AccessDeniedException("You do not own this booking");
        }
        return booking;
    }

    private Booking markConfirmed(Long bookingId, CurrentUser currentUser) {
        Booking booking = requireOwnedOrAdmin(bookingId, currentUser);
        requirePending(booking);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingEventPublisher.publishBookingConfirmed(toConfirmedEvent(booking));
        return booking;
    }

    private BookingConfirmedEvent toConfirmedEvent(Booking booking) {
        return new BookingConfirmedEvent(
                booking.getId(),
                booking.getUserId(),
                booking.getGuestEmail(),
                booking.getRoom().getHotel().getId(),
                booking.getRoom().getHotel().getName(),
                booking.getRoom().getRoomNumber(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                Instant.now()
        );
    }

    private void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Only PENDING bookings can be confirmed");
        }
    }
}
