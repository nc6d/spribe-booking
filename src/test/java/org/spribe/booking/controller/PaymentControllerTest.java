package org.spribe.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.PaymentRequest;
import org.spribe.booking.dto.PaymentResponse;
import org.spribe.booking.model.enumeration.PaymentMethod;
import org.spribe.booking.model.enumeration.PaymentStatus;
import org.spribe.booking.service.PaymentService;
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
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentRequest validPaymentRequest;
    private PaymentResponse mockPaymentResponse;
    private UUID testUserId;
    private UUID testBookingId;
    private UUID testPaymentId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testBookingId = UUID.randomUUID();
        testPaymentId = UUID.randomUUID();

        validPaymentRequest = new PaymentRequest();
        validPaymentRequest.setBookingId(testBookingId);
        validPaymentRequest.setAmount(new BigDecimal("345.00"));
        validPaymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        mockPaymentResponse = new PaymentResponse();
        mockPaymentResponse.setId(testPaymentId);
        mockPaymentResponse.setBookingId(testBookingId);
        mockPaymentResponse.setAmount(new BigDecimal("345.00"));
        mockPaymentResponse.setStatus(PaymentStatus.PENDING);
        mockPaymentResponse.setPaymentMethod(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void createPayment_ValidRequest_ReturnsCreatedPayment() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class), any(UUID.class)))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPaymentId.toString()))
                .andExpect(jsonPath("$.bookingId").value(testBookingId.toString()))
                .andExpect(jsonPath("$.amount").value("345.0"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
    }

    @Test
    void getPayment_ExistingPayment_ReturnsPayment() throws Exception {
        when(paymentService.getPayment(testPaymentId)).thenReturn(mockPaymentResponse);

        mockMvc.perform(get("/api/v1/payments/{id}", testPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPaymentId.toString()))
                .andExpect(jsonPath("$.bookingId").value(testBookingId.toString()))
                .andExpect(jsonPath("$.amount").value("345.0"));
    }

    @Test
    void processPayment_ValidRequest_ReturnsProcessedPayment() throws Exception {
        mockPaymentResponse.setStatus(PaymentStatus.COMPLETED);
        when(paymentService.processPayment(testPaymentId, testUserId))
                .thenReturn(mockPaymentResponse);

        mockMvc.perform(post("/api/v1/payments/{id}/process", testPaymentId)
                        .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPaymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createPayment_InvalidRequest_ReturnsBadRequest() throws Exception {
        validPaymentRequest.setAmount(new BigDecimal("-100.00")); // Invalid amount

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-User-Id", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getPayment_NonExistentPayment_ReturnsNotFound() throws Exception {
        when(paymentService.getPayment(any(UUID.class)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void processPayment_NonExistentPayment_ReturnsNotFound() throws Exception {
        when(paymentService.processPayment(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Payment not found"));

        mockMvc.perform(post("/api/v1/payments/{id}/process", UUID.randomUUID())
                        .header("X-User-Id", testUserId))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void processPayment_UnauthorizedUser_ReturnsForbidden() throws Exception {
        when(paymentService.processPayment(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("User is not authorized to process this payment"));

        mockMvc.perform(post("/api/v1/payments/{id}/process", testPaymentId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
} 