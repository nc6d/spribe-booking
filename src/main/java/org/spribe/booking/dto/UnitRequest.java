package org.spribe.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.spribe.booking.model.enumeration.AccommodationType;

import java.math.BigDecimal;

@Data
public class UnitRequest {
    @NotNull(message = "Number of rooms is required")
    @Min(value = 1, message = "Number of rooms must be at least 1")
    private Integer numberOfRooms;

    @NotNull(message = "Type is required")
    private AccommodationType type;

    @NotNull(message = "Floor is required")
    @Min(value = 0, message = "Floor must be 0 or greater")
    private Integer floor;

    @NotNull(message = "Base price is required")
    @Min(value = 0, message = "Base price must be 0 or greater")
    private BigDecimal basePrice;

    @NotNull(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;
} 