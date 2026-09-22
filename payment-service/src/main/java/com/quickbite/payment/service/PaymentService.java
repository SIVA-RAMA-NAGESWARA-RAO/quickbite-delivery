package com.quickbite.payment.service;

import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.PaymentMethod;
import com.quickbite.payment.entity.PaymentStatus;
import com.quickbite.payment.exception.InvalidPaymentStateException;
import com.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator gateway;

    @Transactional
    public PaymentResponse process(PaymentRequest request) {
        // Idempotency: order-service may retry after a network blip. If a
        // payment already exists for this order, hand back that result
        // instead of charging the customer twice.
        var existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Payment for order {} already exists, returning existing result", request.orderId());
            return toResponse(existing.get());
        }

        Payment payment;
        if (request.method() == PaymentMethod.CASH_ON_DELIVERY) {
            payment = Payment.builder()
                    .orderId(request.orderId())
                    .customerId(request.customerId())
                    .amount(request.amount())
                    .method(request.method())
                    .status(PaymentStatus.SUCCESS)
                    .transactionRef("COD-" + request.orderId())
                    .build();
        } else {
            var result = gateway.charge(request.amount().doubleValue());
            payment = Payment.builder()
                    .orderId(request.orderId())
                    .customerId(request.customerId())
                    .amount(request.amount())
                    .method(request.method())
                    .status(result.approved() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                    .transactionRef(result.transactionRef())
                    .failureReason(result.failureReason())
                    .build();
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Processed payment {} for order {} -> {}", saved.getId(), saved.getOrderId(), saved.getStatus());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order " + orderId));
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for order " + orderId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                    "Cannot refund a payment in status " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Refunded payment {} for order {}", payment.getId(), orderId);
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getCustomerId(), p.getAmount(),
                p.getMethod(), p.getStatus(), p.getTransactionRef(), p.getFailureReason(), p.getCreatedAt());
    }
}
