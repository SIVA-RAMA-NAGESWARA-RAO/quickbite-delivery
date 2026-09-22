package com.quickbite.payment;

import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.PaymentMethod;
import com.quickbite.payment.entity.PaymentStatus;
import com.quickbite.payment.exception.InvalidPaymentStateException;
import com.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.service.PaymentGatewaySimulator;
import com.quickbite.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewaySimulator gateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRepository, gateway);
    }

    @Test
    void process_marksSuccess_whenGatewayApproves() {
        PaymentRequest request = new PaymentRequest(1L, 7L, BigDecimal.valueOf(450), PaymentMethod.UPI);

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(gateway.charge(450.0)).thenReturn(new PaymentGatewaySimulator.GatewayResult(true, "TXN-ABC123", null));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(99L);
            p.setCreatedAt(Instant.now());
            return p;
        });

        var response = paymentService.process(request);

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals("TXN-ABC123", response.transactionRef());
    }

    @Test
    void process_marksFailed_whenGatewayDeclines() {
        PaymentRequest request = new PaymentRequest(2L, 7L, BigDecimal.valueOf(999), PaymentMethod.CARD);

        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.empty());
        when(gateway.charge(999.0)).thenReturn(
                new PaymentGatewaySimulator.GatewayResult(false, "TXN-XYZ999", "Card declined"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(100L);
            p.setCreatedAt(Instant.now());
            return p;
        });

        var response = paymentService.process(request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("Card declined", response.failureReason());
    }

    @Test
    void process_isIdempotent_forSameOrder() {
        Payment existing = Payment.builder().id(5L).orderId(3L).customerId(7L)
                .amount(BigDecimal.valueOf(200)).method(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS).transactionRef("TXN-OLD").createdAt(Instant.now()).build();

        when(paymentRepository.findByOrderId(3L)).thenReturn(Optional.of(existing));

        var response = paymentService.process(new PaymentRequest(3L, 7L, BigDecimal.valueOf(200), PaymentMethod.UPI));

        assertEquals("TXN-OLD", response.transactionRef());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refund_throws_whenPaymentNotSuccessful() {
        Payment failed = Payment.builder().id(6L).orderId(4L).customerId(7L)
                .amount(BigDecimal.valueOf(100)).method(PaymentMethod.CARD)
                .status(PaymentStatus.FAILED).transactionRef("TXN-FAIL").createdAt(Instant.now()).build();

        when(paymentRepository.findByOrderId(4L)).thenReturn(Optional.of(failed));

        assertThrows(InvalidPaymentStateException.class, () -> paymentService.refund(4L));
    }

    @Test
    void refund_throws_whenPaymentDoesNotExist() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> paymentService.refund(999L));
    }
}
