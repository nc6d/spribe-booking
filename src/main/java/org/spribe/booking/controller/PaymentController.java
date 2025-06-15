package org.spribe.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.spribe.booking.dto.PaymentRequest;
import org.spribe.booking.dto.PaymentResponse;
import org.spribe.booking.model.enumeration.PaymentStatus;
import org.spribe.booking.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment management APIs")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a new payment", description = "Creates a new payment for a booking")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody @Valid PaymentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.createPayment(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieves a payment by its ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payments by booking ID", description = "Retrieves all payments for a specific booking")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBooking(bookingId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update payment status", description = "Updates the status of a payment")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, userId, status));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process payment", description = "Processes a pending payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.processPayment(id, userId));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund payment", description = "Refunds a completed payment")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.refundPayment(id, userId));
    }

    @PostMapping("/booking/{bookingId}/cancel")
    @Operation(summary = "Cancel pending payments", description = "Cancels all pending payments for a booking")
    public ResponseEntity<Void> cancelPendingPayments(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId) {
        paymentService.cancelPendingPayments(bookingId, userId);
        return ResponseEntity.ok().build();
    }
} 