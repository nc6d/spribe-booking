package org.spribe.booking.service;

import org.spribe.booking.dto.PaymentRequest;
import org.spribe.booking.dto.PaymentResponse;
import org.spribe.booking.model.enumeration.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest request, UUID userId);
    
    PaymentResponse getPayment(UUID paymentId);
    
    List<PaymentResponse> getPaymentsByBooking(UUID bookingId);
    
    PaymentResponse updatePaymentStatus(UUID paymentId, UUID userId, PaymentStatus status);
    
    PaymentResponse processPayment(UUID paymentId, UUID userId);
    
    PaymentResponse refundPayment(UUID paymentId, UUID userId);
    
    void cancelPendingPayments(UUID bookingId, UUID userId);
} 