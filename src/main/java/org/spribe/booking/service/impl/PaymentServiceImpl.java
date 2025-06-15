package org.spribe.booking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spribe.booking.dto.PaymentRequest;
import org.spribe.booking.dto.PaymentResponse;
import org.spribe.booking.model.*;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.enumeration.PaymentStatus;
import org.spribe.booking.repository.BookingRepository;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.PaymentRepository;
import org.spribe.booking.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, UUID userId) {
        log.info("Creating payment for booking: {}", request.getBookingId());
        
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Booking is not in pending payment status");
        }
        
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .transactionId(request.getTransactionId())
                .build();
        
        payment = paymentRepository.save(payment);
        
        Event event = Event.builder()
                .type(EventType.PAYMENT_CREATED)
                .userId(userId)
                .entityId(payment.getId())
                .description("Payment created for booking: " + booking.getId())
                .build();
        
        eventRepository.save(event);
        
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        log.info("Getting payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBooking(UUID bookingId) {
        log.info("Getting payments for booking: {}", bookingId);
        
        return paymentRepository.findByBookingId(bookingId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Usually can be used by client application to mark payment as FAILED
    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(UUID paymentId, UUID userId, PaymentStatus status) {
        log.info("Updating payment status: {} to {}", paymentId, status);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(status);
        payment = paymentRepository.save(payment);
        
        Event event = Event.builder()
                .type(EventType.PAYMENT_STATUS_UPDATED)
                .userId(userId)
                .entityId(payment.getId())
                .description("Payment status updated to: " + status)
                .build();
        
        eventRepository.save(event);
        
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID paymentId, UUID userId) {
        log.info("Processing payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in pending status");
        }

        // A payment gateway should be integrated here by default.
        // For test purposes, we'll just mark it as completed
        payment.setStatus(PaymentStatus.COMPLETED);
        payment = paymentRepository.save(payment);
        
        // Update booking status
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        
        Event event = Event.builder()
                .type(EventType.PAYMENT_COMPLETED)
                .userId(userId)
                .entityId(payment.getId())
                .description("Payment completed for booking: " + booking.getId())
                .build();
        
        eventRepository.save(event);
        
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, UUID userId) {
        log.info("Refunding payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment is not in completed status");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        
        Event event = Event.builder()
                .type(EventType.PAYMENT_REFUNDED)
                .userId(userId)
                .entityId(payment.getId())
                .description("Payment refunded for booking: " + payment.getBooking().getId())
                .build();
        
        eventRepository.save(event);
        
        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public void cancelPendingPayments(UUID bookingId, UUID userId) {
        log.info("Cancelling pending payments for booking: {}", bookingId);
        
        List<Payment> pendingPayments = paymentRepository.findByBookingIdAndStatus(
                bookingId, PaymentStatus.PENDING);
        
        for (Payment payment : pendingPayments) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            
            Event event = Event.builder()
                    .type(EventType.PAYMENT_CANCELLED)
                    .userId(userId)
                    .entityId(payment.getId())
                    .description("Payment cancelled for booking: " + bookingId)
                    .build();
            
            eventRepository.save(event);
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setBookingId(payment.getBooking().getId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionId(payment.getTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
} 