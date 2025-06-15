package org.spribe.booking.dto;

import lombok.Data;
import org.spribe.booking.model.enumeration.AccommodationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UnitSearchRequest {
    private Integer numberOfRooms;
    private AccommodationType type;
    private Integer floor;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime checkInDate;
    private LocalDateTime checkOutDate;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
} 