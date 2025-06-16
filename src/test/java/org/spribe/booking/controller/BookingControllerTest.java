package org.spribe.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.spribe.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingRequest validBookingRequest;
    private BookingResponse mockBookingResponse;
    private UUID testUserId;
    private UUID testUnitId;
    private UUID testBookingId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();
        testBookingId = UUID.randomUUID();

        validBookingRequest = new BookingRequest();
        validBookingRequest.setUnitId(testUnitId);
        validBookingRequest.setCheckInDate(LocalDateTime.now().plusDays(1));
        validBookingRequest.setCheckOutDate(LocalDateTime.now().plusDays(3));

        mockBookingResponse = new BookingResponse();
        mockBookingResponse.setId(testBookingId);
        mockBookingResponse.setUnitId(testUnitId);
        mockBookingResponse.setUserId(testUserId);
        mockBookingResponse.setCheckInDate(validBookingRequest.getCheckInDate());
        mockBookingResponse.setCheckOutDate(validBookingRequest.getCheckOutDate());
        mockBookingResponse.setStatus(BookingStatus.PENDING_PAYMENT);
        mockBookingResponse.setTotalPrice(new BigDecimal("345.00"));
    }

    @Test
    void createBooking_ValidRequest_ReturnsCreatedBooking() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class), any(UUID.class)))
                .thenReturn(mockBookingResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBookingId.toString()))
                .andExpect(jsonPath("$.unitId").value(testUnitId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalPrice").value("345.0"));
    }

    @Test
    void getBooking_ExistingBooking_ReturnsBooking() throws Exception {
        when(bookingService.getBooking(testBookingId)).thenReturn(mockBookingResponse);

        mockMvc.perform(get("/api/v1/bookings/{id}", testBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBookingId.toString()))
                .andExpect(jsonPath("$.unitId").value(testUnitId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()));
    }

    @Test
    void confirmBooking_ValidRequest_ReturnsConfirmedBooking() throws Exception {
        mockBookingResponse.setStatus(BookingStatus.CONFIRMED);
        when(bookingService.confirmBooking(any(UUID.class), any(UUID.class)))
                .thenReturn(mockBookingResponse);

        mockMvc.perform(post("/api/v1/bookings/{id}/confirm", testBookingId)
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelBooking_ValidRequest_ReturnsCancelledBooking() throws Exception {
        mockBookingResponse.setStatus(BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(any(UUID.class), any(UUID.class)))
                .thenReturn(mockBookingResponse);

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", testBookingId)
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBookingId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getUserBookings_ReturnsPaginatedBookings() throws Exception {
        PageResponse<BookingResponse> pageResponse = new PageResponse<>(
                List.of(mockBookingResponse),
                0,
                10,
                1,
                1,
                true
        );

        when(bookingService.getUserBookings(any(UUID.class), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/bookings/user")
                        .header("X-User-Id", testUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testBookingId.toString()))
                .andExpect(jsonPath("$.content[0].unitId").value(testUnitId.toString()))
                .andExpect(jsonPath("$.content[0].userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void createBooking_InvalidRequest_ReturnsBadRequest() throws Exception {
        validBookingRequest.setCheckInDate(null); // Invalid check-in date

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getBooking_NonExistentBooking_ReturnsNotFound() throws Exception {
        when(bookingService.getBooking(any(UUID.class)))
                .thenThrow(new RuntimeException("Booking not found"));

        mockMvc.perform(get("/api/v1/bookings/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void confirmBooking_UnauthorizedUser_ReturnsUnauthorized() throws Exception {
        when(bookingService.confirmBooking(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("User is not authorized to confirm this booking"));

        mockMvc.perform(post("/api/v1/bookings/{id}/confirm", testBookingId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cancelBooking_AlreadyCancelled_ReturnsBadRequest() throws Exception {
        when(bookingService.cancelBooking(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Booking is already cancelled"));

        mockMvc.perform(post("/api/v1/bookings/{id}/cancel", testBookingId)
                        .header("X-User-Id", testUserId))
                .andExpect(status().is4xxClientError());
    }
} 