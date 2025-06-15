package org.spribe.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.model.*;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.repository.BookingRepository;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private UnitRepository unitRepository;

    @MockBean
    private EventRepository eventRepository;

    @Value("${booking.payment-timeout:15}")
    private int paymentTimeout;

    @Value("${booking.system-markup:15}")
    private int systemMarkup;

    private BookingRequest validBookingRequest;
    private Unit mockUnit;
    private Booking mockBooking;
    private UUID testUserId;
    private UUID testUnitId;
    private UUID testBookingId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();
        testBookingId = UUID.randomUUID();
        now = LocalDateTime.now();

        validBookingRequest = new BookingRequest();
        validBookingRequest.setUnitId(testUnitId);
        validBookingRequest.setCheckInDate(now.plusDays(1));
        validBookingRequest.setCheckOutDate(now.plusDays(3));

        mockUnit = Unit.builder()
                .id(testUnitId)
                .numberOfRooms(2)
                .type(AccommodationType.APARTMENTS)
                .floor(3)
                .basePrice(new BigDecimal("100.00"))
                .totalPrice(new BigDecimal("115.00"))
                .description("Test apartment")
                .available(true)
                .build();

        mockBooking = Booking.builder()
                .id(testBookingId)
                .unit(mockUnit)
                .userId(testUserId)
                .checkInDate(validBookingRequest.getCheckInDate())
                .checkOutDate(validBookingRequest.getCheckOutDate())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalPrice(new BigDecimal("345.00"))
                .paymentDeadline(now.plusMinutes(paymentTimeout))
                .build();
    }

    @Test
    void createBooking_ValidRequest_ReturnsCreatedBooking() {
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));
        when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
        when(bookingRepository.findOverlappingBookings(
                any(UUID.class),
                any(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());


        BookingResponse response = bookingService.createBooking(validBookingRequest, testUserId);

        assertNotNull(response);
        assertEquals(testBookingId, response.getId());
        assertEquals(testUnitId, response.getUnitId());
        assertEquals(testUserId, response.getUserId());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(new BigDecimal("345.00"), response.getTotalPrice());

        verify(unitRepository).save(argThat(unit -> !unit.isAvailable()));
        verify(bookingRepository).save(any(Booking.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.BOOKING_CREATED &&
            event.getEntityId().equals(testBookingId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void createBooking_UnitNotAvailable_ThrowsException() {
        mockUnit.setAvailable(false);
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));

        assertThrows(RuntimeException.class, 
            () -> bookingService.createBooking(validBookingRequest, testUserId));
    }

    @Test
    void createBooking_OverlappingBookings_ThrowsException() {
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));
        when(bookingRepository.findOverlappingBookings(
                any(UUID.class),
                any(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(mockBooking));

        assertThrows(RuntimeException.class, 
            () -> bookingService.createBooking(validBookingRequest, testUserId));
    }

    @Test
    void getBooking_ExistingBooking_ReturnsBooking() {
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));

        BookingResponse response = bookingService.getBooking(testBookingId);

        assertNotNull(response);
        assertEquals(testBookingId, response.getId());
        assertEquals(testUnitId, response.getUnitId());
        assertEquals(testUserId, response.getUserId());
    }

    @Test
    void getBooking_NonExistentBooking_ThrowsException() {
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.getBooking(UUID.randomUUID()));
    }

    @Test
    void cancelBooking_ValidRequest_ReturnsCancelledBooking() {
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        BookingResponse response = bookingService.cancelBooking(testBookingId, testUserId);

        assertNotNull(response);
        assertEquals(testBookingId, response.getId());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());

        verify(unitRepository).save(argThat(Unit::isAvailable));
        verify(bookingRepository).save(any(Booking.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.BOOKING_CANCELLED &&
            event.getEntityId().equals(testBookingId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void cancelBooking_UnauthorizedUser_ThrowsException() {
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));

        assertThrows(RuntimeException.class, 
            () -> bookingService.cancelBooking(testBookingId, UUID.randomUUID()));
    }

    @Test
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        mockBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));

        assertThrows(RuntimeException.class, 
            () -> bookingService.cancelBooking(testBookingId, testUserId));
    }

    @Test
    void processExpiredBookings_ExpiredBookings_CancelsBookings() {
        mockBooking.setPaymentDeadline(now.minusMinutes(1));
        when(bookingRepository.findExpiredBookings(eq(BookingStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());


        bookingService.processExpiredBookings();

        verify(bookingRepository).save(argThat(booking -> 
            booking.getStatus() == BookingStatus.CANCELLED));
        verify(unitRepository).save(argThat(Unit::isAvailable));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.BOOKING_EXPIRED &&
            event.getEntityId().equals(testBookingId) &&
            event.getUserId().equals(testUserId)
        ));
    }
} 