package org.spribe.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.spribe.booking.model.enumeration.AccommodationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UnitResponse {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("numberOfRooms")
    private int numberOfRooms;

    @JsonProperty("type")
    private AccommodationType type;

    @JsonProperty("floor")
    private int floor;

    @JsonProperty("basePrice")
    private BigDecimal basePrice;

    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;

    @JsonProperty("description")
    private String description;

    @JsonProperty("available")
    private boolean available;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}