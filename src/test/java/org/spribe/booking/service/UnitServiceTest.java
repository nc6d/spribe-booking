package org.spribe.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.dto.UnitSearchRequest;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.model.*;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private LocalDateTime now;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache("availableUnits")).clear();
    }

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUnitId = UUID.randomUUID();
        now = LocalDateTime.now();

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

    @Test
    void searchUnits_WithAllCriteria_ReturnsMatchingUnits() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setNumberOfRooms(2);
        request.setType(AccommodationType.APARTMENTS);
        request.setFloor(3);
        request.setMinPrice(new BigDecimal("50.00"));
        request.setMaxPrice(new BigDecimal("200.00"));
        request.setCheckInDate(now.plusDays(1));
        request.setCheckOutDate(now.plusDays(3));
        request.setPage(0);
        request.setSize(10);

        Page<Unit> unitPage = new PageImpl<>(Collections.singletonList(mockUnit));
        when(unitRepository.searchUnits(
                eq(request.getNumberOfRooms()),
                eq(request.getType()),
                eq(request.getFloor()),
                eq(request.getMinPrice().doubleValue()),
                eq(request.getMaxPrice().doubleValue()),
                eq(request.getCheckInDate()),
                eq(request.getCheckOutDate()),
                any(PageRequest.class)
        )).thenReturn(unitPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());

        UnitResponse unitResponse = response.getContent().get(0);
        assertEquals(mockUnit.getId(), unitResponse.getId());
        assertEquals(mockUnit.getNumberOfRooms(), unitResponse.getNumberOfRooms());
        assertEquals(mockUnit.getType(), unitResponse.getType());
        assertEquals(mockUnit.getFloor(), unitResponse.getFloor());
        assertEquals(mockUnit.getBasePrice(), unitResponse.getBasePrice());
        assertEquals(mockUnit.getTotalPrice(), unitResponse.getTotalPrice());
        assertEquals(mockUnit.getDescription(), unitResponse.getDescription());
        assertEquals(mockUnit.isAvailable(), unitResponse.isAvailable());
    }

    @Test
    void searchUnits_WithPartialCriteria_ReturnsMatchingUnits() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setNumberOfRooms(2);
        request.setMinPrice(new BigDecimal("50.00"));
        request.setMaxPrice(new BigDecimal("200.00"));
        request.setPage(0);
        request.setSize(10);

        Page<Unit> unitPage = new PageImpl<>(Collections.singletonList(mockUnit));
        when(unitRepository.searchUnits(
                eq(request.getNumberOfRooms()),
                isNull(),
                isNull(),
                eq(request.getMinPrice().doubleValue()),
                eq(request.getMaxPrice().doubleValue()),
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(unitPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchUnits_WithDateRange_ReturnsAvailableUnits() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setCheckInDate(now.plusDays(1));
        request.setCheckOutDate(now.plusDays(3));
        request.setPage(0);
        request.setSize(10);

        Page<Unit> unitPage = new PageImpl<>(Collections.singletonList(mockUnit));
        when(unitRepository.searchUnits(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(request.getCheckInDate()),
                eq(request.getCheckOutDate()),
                any(PageRequest.class)
        )).thenReturn(unitPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchUnits_WithPriceRange_ReturnsMatchingUnits() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setMinPrice(new BigDecimal("50.00"));
        request.setMaxPrice(new BigDecimal("200.00"));
        request.setPage(0);
        request.setSize(10);

        Page<Unit> unitPage = new PageImpl<>(Collections.singletonList(mockUnit));
        when(unitRepository.searchUnits(
                isNull(),
                isNull(),
                isNull(),
                eq(request.getMinPrice().doubleValue()),
                eq(request.getMaxPrice().doubleValue()),
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(unitPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void searchUnits_WithPagination_ReturnsCorrectPage() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setPage(0);
        request.setSize(5);

        List<Unit> units = Arrays.asList(
                mockUnit,
                Unit.builder()
                        .id(UUID.randomUUID())
                        .numberOfRooms(1)
                        .type(AccommodationType.APARTMENTS)
                        .floor(2)
                        .basePrice(new BigDecimal("80.00"))
                        .totalPrice(new BigDecimal("92.00"))
                        .description("Test studio")
                        .available(true)
                        .build()
        );

        Page<Unit> unitPage = new PageImpl<>(units, PageRequest.of(0, 5), 10);
        when(unitRepository.searchUnits(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(unitPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(10, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertFalse(response.isLast());
    }

    @Test
    void searchUnits_NoResults_ReturnsEmptyPage() {
        UnitSearchRequest request = new UnitSearchRequest();
        request.setPage(0);
        request.setSize(10);

        Page<Unit> emptyPage = new PageImpl<>(Collections.emptyList());
        when(unitRepository.searchUnits(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(emptyPage);

        PageResponse<UnitResponse> response = unitService.searchUnits(request);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());
    }
} 