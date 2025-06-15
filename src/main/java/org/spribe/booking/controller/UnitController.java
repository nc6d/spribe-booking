package org.spribe.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.dto.UnitSearchRequest;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.service.UnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
@Tag(name = "Unit Management", description = "APIs for managing accommodation units")
public class UnitController {
    private final UnitService unitService;

    @PostMapping
    @Operation(summary = "Create a new unit", description = "Creates a new accommodation unit with the specified properties")
    public ResponseEntity<UnitResponse> createUnit(
            @RequestBody @Valid UnitRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(unitService.createUnit(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get unit by ID", description = "Retrieves a specific unit by its ID")
    public ResponseEntity<UnitResponse> getUnit(@PathVariable UUID id) {
        return ResponseEntity.ok(unitService.getUnit(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update unit", description = "Updates an existing unit's properties")
    public ResponseEntity<UnitResponse> updateUnit(
            @PathVariable UUID id,
            @RequestBody UnitRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(unitService.updateUnit(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete unit", description = "Deletes a unit by its ID")
    public ResponseEntity<Void> deleteUnit(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        unitService.deleteUnit(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    @Operation(summary = "Search units", description = "Searches for units based on various criteria")
    public ResponseEntity<PageResponse<UnitResponse>> searchUnits(@RequestBody UnitSearchRequest request) {
        return ResponseEntity.ok(unitService.searchUnits(request));
    }

    @GetMapping("/available/count")
    @Operation(summary = "Get available units count", description = "Returns the total number of available units")
    public ResponseEntity<Long> getAvailableUnitsCount() {
        return ResponseEntity.ok(unitService.getAvailableUnitsCount());
    }
}