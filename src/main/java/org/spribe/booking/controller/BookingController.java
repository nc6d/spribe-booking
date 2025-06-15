package org.spribe.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "APIs for managing bookings")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking", description = "Creates a new booking for a unit")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestBody @Valid BookingRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.createBooking(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Retrieves a specific booking by its ID")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "Confirms a pending booking after payment")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, userId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancels an existing booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }

    @GetMapping("/user")
    @Operation(summary = "Get user bookings", description = "Retrieves all bookings for a specific user")
    public ResponseEntity<PageResponse<BookingResponse>> getUserBookings(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId, page, size));
    }
} 