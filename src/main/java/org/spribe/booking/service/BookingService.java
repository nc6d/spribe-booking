package org.spribe.booking.service;

import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.dto.PageResponse;

import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, UUID userId);
    BookingResponse getBooking(UUID bookingId);
    BookingResponse confirmBooking(UUID bookingId, UUID userId);
    BookingResponse cancelBooking(UUID bookingId, UUID userId);
    PageResponse<BookingResponse> getUserBookings(UUID userId, int page, int size);
    void processExpiredBookings();
    void processCompletedBookings();
} 