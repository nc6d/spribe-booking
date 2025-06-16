package org.spribe.booking.repository;

import org.spribe.booking.model.Booking;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("SELECT b FROM Booking b WHERE b.unit.id = :unitId " +
           "AND b.status IN :statuses " +
           "AND b.checkInDate <= :checkOutDate " +
           "AND b.checkOutDate >= :checkInDate")
    List<Booking> findOverlappingBookings(
            @Param("unitId") UUID unitId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("checkInDate") LocalDateTime checkInDate,
            @Param("checkOutDate") LocalDateTime checkOutDate
    );

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.checkOutDate <= :now")
    List<Booking> findCompletedBookings(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.paymentDeadline < :now")
    List<Booking> findExpiredBookings(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now
    );

    Page<Booking> findByUserId(UUID userId, Pageable pageable);
} 