package org.spribe.booking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.model.Booking;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.spribe.booking.model.Event;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.Unit;
import org.spribe.booking.repository.BookingRepository;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UnitRepository;
import org.spribe.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UnitRepository unitRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Value("${booking.payment-timeout:15}")
    private int paymentTimeout;

    @Value("${booking.system-markup:15}")
    private int systemMarkup;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, UUID userId) {
        log.info("Creating booking for unit {} by user {}", request.getUnitId(), userId);
        
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        if (!unit.isAvailable()) {
            throw new RuntimeException("Unit is not available");
        }

        if (request.getCheckInDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invalid check-in date " + request.getCheckInDate());
        }

        if (request.getCheckOutDate().isBefore(LocalDateTime.now()) ||
                request.getCheckOutDate().isBefore(request.getCheckInDate())) {
            throw new RuntimeException("Invalid check-out date " + request.getCheckInDate());
        }

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                request.getUnitId(),
                Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (!overlappingBookings.isEmpty()) {
            throw new RuntimeException("Unit is already booked for the selected dates");
        }

        BigDecimal totalPrice = unit.getBasePrice()
                .multiply(BigDecimal.valueOf(1 + systemMarkup / 100.0))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime paymentDeadline = LocalDateTime.now().plusMinutes(paymentTimeout);

        // Mark unit as unavailable
        unit.setAvailable(false);
        unit = unitRepository.save(unit);
        log.info("Marked unit {} as unavailable", unit.getId());

        Booking booking = Booking.builder()
                .unit(unit)
                .userId(userId)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentDeadline(paymentDeadline)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Created booking {} with total price {} and payment deadline {}", 
                booking.getId(), totalPrice, paymentDeadline);

        // Create event
        Event event = Event.builder()
                .type(EventType.BOOKING_CREATED)
                .entityId(booking.getId())
                .userId(userId)
                .description("Booking created with total price " + totalPrice + " and payment deadline " + paymentDeadline)
                .build();
        eventRepository.save(event);

        return objectMapper.convertValue(booking, BookingResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID id) {
        log.info("Getting booking: {}", id);
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        return objectMapper.convertValue(booking, BookingResponse.class);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID id, UUID userId) {
        log.info("Confirming booking: {}", id);
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("User is not authorized to confirm this booking");
        }
        
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Booking is not in pending status");
        }
        
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        
        // Unit remains unavailable as it's confirmed
        log.info("Booking {} confirmed, unit {} remains unavailable", booking.getId(), booking.getUnit().getId());
        
        Event event = Event.builder()
                .type(EventType.BOOKING_CONFIRMED)
                .entityId(booking.getId())
                .userId(userId)
                .description("Booking confirmed")
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(booking, BookingResponse.class);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID id, UUID userId) {
        log.info("Cancelling booking: {}", id);
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUserId().equals(userId)) {
            throw new RuntimeException("User is not authorized to cancel this booking");
        }
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        
        // Mark unit as available again
        Unit unit = booking.getUnit();
        unit.setAvailable(true);
        unitRepository.save(unit);
        log.info("Marked unit {} as available after booking cancellation", unit.getId());
        
        Event event = Event.builder()
                .type(EventType.BOOKING_CANCELLED)
                .entityId(booking.getId())
                .userId(userId)
                .description("Booking cancelled")
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(booking, BookingResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(UUID userId, int page, int size) {
        log.info("Getting bookings for user: {}, page: {}, size: {}", userId, page, size);
        
        Page<Booking> bookings = bookingRepository.findByUserId(userId, PageRequest.of(page, size));
        
        List<BookingResponse> bookingResponses = bookings.getContent().stream()
                .map(booking -> objectMapper.convertValue(booking, BookingResponse.class))
                .toList();
        
        return new PageResponse<>(
                bookingResponses,
                bookings.getNumber(),
                bookings.getSize(),
                bookings.getTotalElements(),
                bookings.getTotalPages(),
                bookings.isLast()
        );
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000) // Run every minute
    public void processExpiredBookings() {
        log.info("Processing expired bookings");
        
        LocalDateTime now = LocalDateTime.now();
        var expiredBookings = bookingRepository.findExpiredBookings(
                BookingStatus.PENDING_PAYMENT, now);
        
        log.info("Found {} expired bookings", expiredBookings.size());
        
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            
            Unit unit = booking.getUnit();
            unit.setAvailable(true);
            unitRepository.save(unit);
            log.info("Marked unit {} as available after booking expiration", unit.getId());
            
            Event event = Event.builder()
                    .type(EventType.BOOKING_EXPIRED)
                    .entityId(booking.getId())
                    .userId(booking.getUserId())
                    .description("Booking expired and cancelled")
                    .build();
            eventRepository.save(event);
        }
    }
} 