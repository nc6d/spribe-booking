package org.spribe.booking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.dto.UnitSearchRequest;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.model.Event;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.Unit;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UnitRepository;
import org.spribe.booking.service.UnitService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "availableUnits", allEntries = true)
    public UnitResponse createUnit(UnitRequest request, UUID userId) {
        log.info("Creating unit by user: {} - Cache will be evicted", userId);
        
        Unit unit = Unit.builder()
                .numberOfRooms(request.getNumberOfRooms())
                .type(request.getType())
                .floor(request.getFloor())
                .basePrice(request.getBasePrice())
                .description(request.getDescription())
                .available(true)
                .build();
        
        unit = unitRepository.save(unit);
        
        Event event = Event.builder()
                .type(EventType.UNIT_CREATED)
                .entityId(unit.getId())
                .userId(userId)
                .description("Unit created: " + unit.getId())
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(unit, UnitResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnit(UUID id) {
        log.info("Getting unit: {}", id);
        
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        
        return objectMapper.convertValue(unit, UnitResponse.class);
    }

    @Override
    @Transactional
    @CacheEvict(value = "availableUnits", allEntries = true)
    public UnitResponse updateUnit(UUID id, UnitRequest request, UUID userId) {
        log.info("Updating unit: {} by user: {} - Cache will be evicted", id, userId);
        
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        
        unit.setNumberOfRooms(request.getNumberOfRooms());
        unit.setType(request.getType());
        unit.setFloor(request.getFloor());
        unit.setBasePrice(request.getBasePrice());
        unit.setDescription(request.getDescription());
        
        unit = unitRepository.save(unit);
        
        Event event = Event.builder()
                .type(EventType.UNIT_UPDATED)
                .entityId(unit.getId())
                .userId(userId)
                .description("Unit updated: " + unit.getId())
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(unit, UnitResponse.class);
    }

    @Override
    @Transactional
    @CacheEvict(value = "availableUnits", allEntries = true)
    public void deleteUnit(UUID id, UUID userId) {
        log.info("Deleting unit: {} by user: {} - Cache will be evicted", id, userId);
        
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        
        unitRepository.delete(unit);
        
        Event event = Event.builder()
                .type(EventType.UNIT_DELETED)
                .entityId(unit.getId())
                .userId(userId)
                .description("Unit deleted: " + unit.getId())
                .build();
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnitResponse> searchUnits(UnitSearchRequest request) {
        log.info("Searching units with criteria: {}", request);
        
        Page<Unit> units = unitRepository.searchUnits(
                request.getNumberOfRooms(),
                request.getType(),
                request.getFloor(),
                request.getMinPrice().doubleValue(),
                request.getMaxPrice().doubleValue(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                PageRequest.of(request.getPage(), request.getSize())
        );
        
        List<UnitResponse> unitResponses = units.getContent().stream()
                .map(unit -> objectMapper.convertValue(unit, UnitResponse.class))
                .toList();
        
        return new PageResponse<>(
                unitResponses,
                units.getNumber(),
                units.getSize(),
                units.getTotalElements(),
                units.getTotalPages(),
                units.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "availableUnits", key = "'count'")
    public long getAvailableUnitsCount() {
        log.info("Cache miss - Getting count of available units from database");
        long count = unitRepository.countAvailableUnits();
        log.info("Found {} available units in database", count);
        return count;
    }
} 