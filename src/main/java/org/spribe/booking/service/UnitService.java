package org.spribe.booking.service;

import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.dto.UnitSearchRequest;
import org.spribe.booking.dto.PageResponse;

import java.util.UUID;

public interface UnitService {
    UnitResponse createUnit(UnitRequest request, UUID userId);
    UnitResponse getUnit(UUID id);
    UnitResponse updateUnit(UUID id, UnitRequest request, UUID userId);
    void deleteUnit(UUID id, UUID userId);
    PageResponse<UnitResponse> searchUnits(UnitSearchRequest request);
    long getAvailableUnitsCount();
} 