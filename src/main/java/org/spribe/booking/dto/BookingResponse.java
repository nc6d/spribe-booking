package org.spribe.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.spribe.booking.model.enumeration.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingResponse {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("unitId")
    private UUID unitId;

    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("checkInDate")
    private LocalDateTime checkInDate;

    @JsonProperty("checkOutDate")
    private LocalDateTime checkOutDate;

    @JsonProperty("totalPrice")
    private BigDecimal totalPrice;

    @JsonProperty("status")
    private BookingStatus status;

    @JsonProperty("paymentDeadline")
    private LocalDateTime paymentDeadline;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}