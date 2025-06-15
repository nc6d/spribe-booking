package org.spribe.booking.repository;

import org.spribe.booking.model.Unit;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {
    @Query("SELECT u FROM Unit u WHERE " +
           "(:numberOfRooms IS NULL OR u.numberOfRooms = :numberOfRooms) AND " +
           "(:type IS NULL OR u.type = :type) AND " +
           "(:floor IS NULL OR u.floor = :floor) AND " +
           "(:minPrice IS NULL OR u.totalPrice >= :minPrice) AND " +
           "(:maxPrice IS NULL OR u.totalPrice <= :maxPrice) AND " +
           "u.available = true AND " +
           "NOT EXISTS (SELECT b FROM Booking b WHERE b.unit = u AND " +
           "((b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate) AND " +
           "b.status IN ('CONFIRMED', 'PENDING_PAYMENT')))")
    Page<Unit> searchUnits(
            @Param("numberOfRooms") Integer numberOfRooms,
            @Param("type") AccommodationType type,
            @Param("floor") Integer floor,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("checkInDate") LocalDateTime checkInDate,
            @Param("checkOutDate") LocalDateTime checkOutDate,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.available = true")
    Long countAvailableUnits();
} 