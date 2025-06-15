package org.spribe.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingRequest {
    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Check-in date is required")
    @Future
    private LocalDateTime checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future
    private LocalDateTime checkOutDate;
} 