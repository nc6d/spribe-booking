package org.spribe.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.spribe.booking.model.enumeration.AccommodationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "units")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer numberOfRooms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccommodationType type;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 