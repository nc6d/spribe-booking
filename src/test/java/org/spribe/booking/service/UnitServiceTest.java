package org.spribe.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.spribe.booking.model.Event;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.Unit;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
class UnitServiceTest {

    @Autowired
    private UnitService unitService;

    @MockBean
    private UnitRepository unitRepository;

    @MockBean
    private EventRepository eventRepository;

    @Autowired
    private CacheManager cacheManager;

    private UnitRequest validUnitRequest;
    private Unit mockUnit;
    private UUID testUserId;
    private UUID testUnitId;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache("availableUnits")).clear();
    }

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();

        validUnitRequest = new UnitRequest();
        validUnitRequest.setNumberOfRooms(2);
        validUnitRequest.setType(AccommodationType.APARTMENTS);
        validUnitRequest.setFloor(3);
        validUnitRequest.setBasePrice(new BigDecimal("100.00"));
        validUnitRequest.setDescription("Test apartment description");

        mockUnit = Unit.builder()
                .id(testUnitId)
                .numberOfRooms(2)
                .type(AccommodationType.APARTMENTS)
                .floor(3)
                .basePrice(new BigDecimal("100.00"))
                .totalPrice(new BigDecimal("115.00"))
                .description("Test apartment description")
                .available(true)
                .build();
    }

    @Test
    void createUnit_ValidRequest_ReturnsCreatedUnit() {
        when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        UnitResponse response = unitService.createUnit(validUnitRequest, testUserId);

        assertNotNull(response);
        assertEquals(testUnitId, response.getId());
        assertEquals(2, response.getNumberOfRooms());
        assertEquals(AccommodationType.APARTMENTS, response.getType());
        assertEquals(3, response.getFloor());
        assertEquals(new BigDecimal("100.00"), response.getBasePrice());
        assertEquals(new BigDecimal("115.00"), response.getTotalPrice());
        assertTrue(response.isAvailable());

        verify(unitRepository).save(any(Unit.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.UNIT_CREATED &&
            event.getEntityId().equals(testUnitId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void getUnit_ExistingUnit_ReturnsUnit() {
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));

        UnitResponse response = unitService.getUnit(testUnitId);

        assertNotNull(response);
        assertEquals(testUnitId, response.getId());
        assertEquals(2, response.getNumberOfRooms());
        assertEquals(AccommodationType.APARTMENTS, response.getType());
    }

    @Test
    void getUnit_NonExistentUnit_ThrowsException() {
        when(unitRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> unitService.getUnit(UUID.randomUUID()));
    }

    @Test
    void updateUnit_ValidRequest_ReturnsUpdatedUnit() {
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));
        when(unitRepository.save(any(Unit.class))).thenReturn(mockUnit);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        UnitResponse response = unitService.updateUnit(testUnitId, validUnitRequest, testUserId);

        assertNotNull(response);
        assertEquals(testUnitId, response.getId());
        assertEquals(2, response.getNumberOfRooms());
        assertEquals(AccommodationType.APARTMENTS, response.getType());

        verify(unitRepository).save(any(Unit.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.UNIT_UPDATED &&
            event.getEntityId().equals(testUnitId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void deleteUnit_ExistingUnit_DeletesUnit() {
        when(unitRepository.findById(testUnitId)).thenReturn(Optional.of(mockUnit));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        unitService.deleteUnit(testUnitId, testUserId);

        verify(unitRepository).delete(mockUnit);
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.UNIT_DELETED &&
            event.getEntityId().equals(testUnitId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void getAvailableUnitsCount_ReturnsCorrectCount() {
        when(unitRepository.countAvailableUnits()).thenReturn(100L);

        long count = unitService.getAvailableUnitsCount();

        assertEquals(100L, count);
        verify(unitRepository).countAvailableUnits();
    }
    @Test
    void getAvailableUnitsCount_CachesResultAfterFirstCall() {
        when(unitRepository.countAvailableUnits()).thenReturn(100L);

        // 1st call should hit the DB, and 2nd call should use cache
        long firstCall = unitService.getAvailableUnitsCount();
        long secondCall = unitService.getAvailableUnitsCount();

        assertEquals(100L, firstCall);
        assertEquals(100L, secondCall);

        verify(unitRepository, times(1)).countAvailableUnits();
    }



    @Test
    void createUnit_InvalidRequest_ThrowsException() {
        validUnitRequest.setNumberOfRooms(0); // Invalid number of rooms

        assertThrows(Exception.class, () -> unitService.createUnit(validUnitRequest, testUserId));
    }

    @Test
    void updateUnit_NonExistentUnit_ThrowsException() {
        when(unitRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
            () -> unitService.updateUnit(UUID.randomUUID(), validUnitRequest, testUserId));
    }
} 