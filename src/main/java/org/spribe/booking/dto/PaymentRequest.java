package org.spribe.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.spribe.booking.model.enumeration.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull
    private UUID bookingId;
    
    @NotNull
    @Positive
    private BigDecimal amount;
    
    @NotNull
    private PaymentMethod paymentMethod;

    @org.hibernate.validator.constraints.UUID
    private String transactionId;
} 