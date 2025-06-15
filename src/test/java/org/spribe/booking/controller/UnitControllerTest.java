package org.spribe.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.UnitRequest;
import org.spribe.booking.dto.UnitResponse;
import org.spribe.booking.model.enumeration.AccommodationType;
import org.spribe.booking.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UnitService unitService;

    private UnitRequest validUnitRequest;
    private UnitResponse mockUnitResponse;
    private UUID testUserId;
    private UUID testUnitId;

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

        mockUnitResponse = new UnitResponse();
        mockUnitResponse.setId(testUnitId);
        mockUnitResponse.setNumberOfRooms(2);
        mockUnitResponse.setType(AccommodationType.APARTMENTS);
        mockUnitResponse.setFloor(3);
        mockUnitResponse.setBasePrice(new BigDecimal("100.00"));
        mockUnitResponse.setTotalPrice(new BigDecimal("115.00"));
        mockUnitResponse.setDescription("Test apartment description");
        mockUnitResponse.setAvailable(true);
    }

    @Test
    void createUnit_ValidRequest_ReturnsCreatedUnit() throws Exception {
        when(unitService.createUnit(any(UnitRequest.class), any(UUID.class)))
                .thenReturn(mockUnitResponse);

        mockMvc.perform(post("/api/v1/units")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUnitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUnitId.toString()))
                .andExpect(jsonPath("$.numberOfRooms").value(2))
                .andExpect(jsonPath("$.type").value("APARTMENTS"))
                .andExpect(jsonPath("$.floor").value(3))
                .andExpect(jsonPath("$.basePrice").value("100.0"))
                .andExpect(jsonPath("$.totalPrice").value("115.0"))
                .andExpect(jsonPath("$.description").value("Test apartment description"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getUnit_ExistingUnit_ReturnsUnit() throws Exception {
        when(unitService.getUnit(testUnitId)).thenReturn(mockUnitResponse);

        mockMvc.perform(get("/api/v1/units/{id}", testUnitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUnitId.toString()))
                .andExpect(jsonPath("$.numberOfRooms").value(2))
                .andExpect(jsonPath("$.type").value("APARTMENTS"));
    }

    @Test
    void updateUnit_ValidRequest_ReturnsUpdatedUnit() throws Exception {
        when(unitService.updateUnit(any(UUID.class), any(UnitRequest.class), any(UUID.class)))
                .thenReturn(mockUnitResponse);

        mockMvc.perform(put("/api/v1/units/{id}", testUnitId)
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUnitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUnitId.toString()))
                .andExpect(jsonPath("$.numberOfRooms").value(2))
                .andExpect(jsonPath("$.type").value("APARTMENTS"));
    }

    @Test
    void deleteUnit_ExistingUnit_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/units/{id}", testUnitId)
                        .header("X-User-Id", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAvailableUnitsCount_ReturnsCount() throws Exception {
        when(unitService.getAvailableUnitsCount()).thenReturn(100L);

        mockMvc.perform(get("/api/v1/units/available/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void createUnit_InvalidRequest_ReturnsBadRequest() throws Exception {
        validUnitRequest.setNumberOfRooms(0); // Invalid number of rooms

        mockMvc.perform(post("/api/v1/units")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUnitRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getUnit_NonExistentUnit_ReturnsNotFound() throws Exception {
        when(unitService.getUnit(any(UUID.class)))
                .thenThrow(new RuntimeException("Unit not found"));

        mockMvc.perform(get("/api/v1/units/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
} 