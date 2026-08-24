package com.stayfinder.booking.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayfinder.booking.dto.CreateBookingRequest;
import com.stayfinder.booking.dto.HotelRequest;
import com.stayfinder.booking.dto.RoomRequest;
import com.stayfinder.booking.event.BookingEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingEventPublisher bookingEventPublisher;

    @Test
    void publicHotelCatalogDoesNotRequireJwt() throws Exception {
        mockMvc.perform(get("/api/bookings/hotels"))
                .andExpect(status().isOk());
    }

    @Test
    void customerCanBookAvailableRoom() throws Exception {
        long roomId = createRoomAsAdmin();
        LocalDate checkIn = LocalDate.of(2026, 9, 10);
        LocalDate checkOut = LocalDate.of(2026, 9, 12);

        mockMvc.perform(post("/api/bookings")
                        .with(customer(42))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(roomId, checkIn, checkOut))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.roomId").value(roomId));

        verifyNoInteractions(bookingEventPublisher);
    }

    @Test
    void overlappingBookingIsRejected() throws Exception {
        long roomId = createRoomAsAdmin();
        LocalDate checkIn = LocalDate.of(2026, 10, 1);
        LocalDate checkOut = LocalDate.of(2026, 10, 5);

        mockMvc.perform(post("/api/bookings")
                        .with(customer(7))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(roomId, checkIn, checkOut))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                        .with(customer(8))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(roomId, LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void cancelledBookingIsIgnoredForAvailability() throws Exception {
        long roomId = createRoomAsAdmin();
        LocalDate checkIn = LocalDate.of(2026, 11, 1);
        LocalDate checkOut = LocalDate.of(2026, 11, 4);

        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .with(customer(11))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(roomId, checkIn, checkOut))))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel").with(customer(11)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/bookings/rooms/" + roomId + "/availability")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(post("/api/bookings")
                        .with(customer(12))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(roomId, checkIn, checkOut))))
                .andExpect(status().isCreated());
    }

    @Test
    void missingJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2)))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerCannotCreateHotel() throws Exception {
        mockMvc.perform(post("/api/bookings/hotels")
                        .with(customer(3))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HotelRequest("Nope", "Hyderabad", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void customerCannotConfirmSomeoneElsesBooking() throws Exception {
        long roomId = createRoomAsAdmin();
        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .with(customer(21))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(roomId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)))))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/bookings/" + bookingId + "/confirm").with(customer(99)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanReadBookingById() throws Exception {
        long roomId = createRoomAsAdmin();
        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .with(customer(21))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(roomId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)))))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/bookings/" + bookingId).with(customer(21)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));

        mockMvc.perform(get("/api/bookings/" + bookingId).with(customer(99)))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmPublishesBookingConfirmedEvent() throws Exception {
        long roomId = createRoomAsAdmin();
        MvcResult created = mockMvc.perform(post("/api/bookings")
                        .with(customer(21))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(roomId, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22)))))
                .andExpect(status().isCreated())
                .andReturn();
        long bookingId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/bookings/" + bookingId + "/confirm").with(customer(21)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingEventPublisher).publishBookingConfirmed(argThat(event ->
                event.bookingId().equals(bookingId)
                        && event.userId().equals(21L)
                        && "101".equals(event.roomNumber())
        ));
    }

    private long createRoomAsAdmin() throws Exception {
        MvcResult hotelResult = mockMvc.perform(post("/api/bookings/hotels")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new HotelRequest("Hotel " + UUID.randomUUID(), "Hyderabad", "Test"))))
                .andExpect(status().isCreated())
                .andReturn();
        long hotelId = objectMapper.readTree(hotelResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult roomResult = mockMvc.perform(post("/api/bookings/rooms")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RoomRequest(hotelId, "101", "STANDARD", new BigDecimal("3000.00")))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode room = objectMapper.readTree(roomResult.getResponse().getContentAsString());
        return room.get("id").asLong();
    }

    private RequestPostProcessor customer(long userId) {
        return jwt().jwt(jwt -> jwt
                        .subject(String.valueOf(userId))
                        .claim("email", "customer" + userId + "@stayfinder.local")
                        .claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private RequestPostProcessor admin() {
        return jwt().jwt(jwt -> jwt
                        .subject("1")
                        .claim("email", "admin@stayfinder.local")
                        .claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
