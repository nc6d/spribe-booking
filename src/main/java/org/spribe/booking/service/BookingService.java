package org.spribe.booking.service;

import org.spribe.booking.dto.BookingRequest;
import org.spribe.booking.dto.BookingResponse;
import org.spribe.booking.dto.PageResponse;

import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, UUID userId);
    BookingResponse getBooking(UUID id);
    BookingResponse confirmBooking(UUID id, UUID userId);
    BookingResponse cancelBooking(UUID id, UUID userId);
    PageResponse<BookingResponse> getUserBookings(UUID userId, int page, int size);
    void processExpiredBookings();
} 