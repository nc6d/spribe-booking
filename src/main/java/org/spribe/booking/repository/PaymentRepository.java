package org.spribe.booking.repository;

import org.spribe.booking.model.Payment;
import org.spribe.booking.model.enumeration.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBookingId(UUID bookingId);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.status = :status")
    List<Payment> findByBookingIdAndStatus(
            @Param("bookingId") UUID bookingId,
            @Param("status") PaymentStatus status
    );
} 