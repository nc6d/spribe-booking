package org.spribe.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.PaymentRequest;
import org.spribe.booking.dto.PaymentResponse;
import org.spribe.booking.model.*;
import org.spribe.booking.model.enumeration.BookingStatus;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.enumeration.PaymentMethod;
import org.spribe.booking.model.enumeration.PaymentStatus;
import org.spribe.booking.repository.BookingRepository;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private EventRepository eventRepository;

    private PaymentRequest validPaymentRequest;
    private Payment mockPayment;
    private Booking mockBooking;
    private UUID testUserId;
    private UUID testBookingId;
    private UUID testPaymentId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testBookingId = UUID.randomUUID();
        testPaymentId = UUID.randomUUID();
        now = LocalDateTime.now();

        validPaymentRequest = new PaymentRequest();
        validPaymentRequest.setBookingId(testBookingId);
        validPaymentRequest.setAmount(new BigDecimal("345.00"));
        validPaymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        mockBooking = Booking.builder()
                .id(testBookingId)
                .userId(testUserId)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalPrice(new BigDecimal("345.00"))
                .paymentDeadline(now.plusMinutes(15))
                .build();

        mockPayment = Payment.builder()
                .id(testPaymentId)
                .booking(mockBooking)
                .amount(new BigDecimal("345.00"))
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
    }

    @Test
    void createPayment_ValidRequest_ReturnsCreatedPayment() {
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        PaymentResponse response = paymentService.createPayment(validPaymentRequest, testUserId);

        assertNotNull(response);
        assertEquals(testPaymentId, response.getId());
        assertEquals(testBookingId, response.getBookingId());
        assertEquals(new BigDecimal("345.00"), response.getAmount());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(PaymentMethod.CREDIT_CARD, response.getPaymentMethod());

        verify(paymentRepository).save(any(Payment.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.PAYMENT_CREATED &&
            event.getEntityId().equals(testPaymentId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void createPayment_NonExistentBooking_ThrowsException() {
        when(bookingRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
            () -> paymentService.createPayment(validPaymentRequest, testUserId));
    }

    @Test
    void createPayment_InvalidBookingStatus_ThrowsException() {
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(testBookingId)).thenReturn(Optional.of(mockBooking));

        assertThrows(RuntimeException.class, 
            () -> paymentService.createPayment(validPaymentRequest, testUserId));
    }

    @Test
    void getPayment_ExistingPayment_ReturnsPayment() {
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));

        PaymentResponse response = paymentService.getPayment(testPaymentId);

        assertNotNull(response);
        assertEquals(testPaymentId, response.getId());
        assertEquals(testBookingId, response.getBookingId());
        assertEquals(new BigDecimal("345.00"), response.getAmount());
    }

    @Test
    void getPayment_NonExistentPayment_ThrowsException() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentService.getPayment(UUID.randomUUID()));
    }

    @Test
    void getPaymentsByBooking_ReturnsPaymentsList() {
        List<Payment> payments = Arrays.asList(mockPayment);
        when(paymentRepository.findByBookingId(testBookingId)).thenReturn(payments);

        List<PaymentResponse> responses = paymentService.getPaymentsByBooking(testBookingId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testPaymentId, responses.get(0).getId());
        assertEquals(testBookingId, responses.get(0).getBookingId());
        assertEquals(new BigDecimal("345.00"), responses.get(0).getAmount());
    }

    @Test
    void updatePaymentStatus_ValidRequest_ReturnsUpdatedPayment() {
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        PaymentResponse response = paymentService.updatePaymentStatus(
                testPaymentId, testUserId, PaymentStatus.FAILED);

        assertNotNull(response);
        assertEquals(testPaymentId, response.getId());
        assertEquals(PaymentStatus.FAILED, response.getStatus());

        verify(paymentRepository).save(any(Payment.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.PAYMENT_STATUS_UPDATED &&
            event.getEntityId().equals(testPaymentId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void processPayment_ValidRequest_ReturnsProcessedPayment() {
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        PaymentResponse response = paymentService.processPayment(testPaymentId, testUserId);

        assertNotNull(response);
        assertEquals(testPaymentId, response.getId());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());

        verify(paymentRepository).save(argThat(payment -> 
            payment.getStatus() == PaymentStatus.COMPLETED));
        verify(bookingRepository).save(argThat(booking -> 
            booking.getStatus() == BookingStatus.CONFIRMED));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.PAYMENT_COMPLETED &&
            event.getEntityId().equals(testPaymentId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void processPayment_NonExistentPayment_ThrowsException() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
            () -> paymentService.processPayment(UUID.randomUUID(), testUserId));
    }

    @Test
    void processPayment_AlreadyProcessed_ThrowsException() {
        mockPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(RuntimeException.class, 
            () -> paymentService.processPayment(testPaymentId, testUserId));
    }

    @Test
    void refundPayment_ValidRequest_ReturnsRefundedPayment() {
        mockPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        PaymentResponse response = paymentService.refundPayment(testPaymentId, testUserId);

        assertNotNull(response);
        assertEquals(testPaymentId, response.getId());
        assertEquals(PaymentStatus.REFUNDED, response.getStatus());

        verify(paymentRepository).save(argThat(payment -> 
            payment.getStatus() == PaymentStatus.REFUNDED));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.PAYMENT_REFUNDED &&
            event.getEntityId().equals(testPaymentId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void refundPayment_NonCompletedPayment_ThrowsException() {
        when(paymentRepository.findById(testPaymentId)).thenReturn(Optional.of(mockPayment));

        assertThrows(RuntimeException.class, 
            () -> paymentService.refundPayment(testPaymentId, testUserId));
    }

    @Test
    void cancelPendingPayments_ValidRequest_CancelsPayments() {
        List<Payment> pendingPayments = Arrays.asList(mockPayment);
        when(paymentRepository.findByBookingIdAndStatus(
                testBookingId, PaymentStatus.PENDING)).thenReturn(pendingPayments);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        paymentService.cancelPendingPayments(testBookingId, testUserId);

        verify(paymentRepository).save(argThat(payment -> 
            payment.getStatus() == PaymentStatus.CANCELLED));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.PAYMENT_CANCELLED &&
            event.getEntityId().equals(testPaymentId) &&
            event.getUserId().equals(testUserId)
        ));
    }
} 