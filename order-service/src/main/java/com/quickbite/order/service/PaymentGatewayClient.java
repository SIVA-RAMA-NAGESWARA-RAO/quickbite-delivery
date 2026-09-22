package com.quickbite.order.service;

import com.quickbite.order.client.PaymentClient;
import com.quickbite.order.dto.external.PaymentRequestDto;
import com.quickbite.order.dto.external.PaymentResponseDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wraps the Feign call to payment-service with a circuit breaker. If
 * payment-service is down or erroring for a sustained period, the breaker
 * trips and every further request fails fast (via the fallback below)
 * instead of piling up threads waiting on a dead dependency - exactly the
 * kind of protection QuickBite needs during a lunch-hour traffic spike.
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private final PaymentClient paymentClient;

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponseDto charge(PaymentRequestDto request) {
        return paymentClient.processPayment(request);
    }

    @SuppressWarnings("unused") // invoked reflectively by Resilience4j on failure/open-circuit
    private PaymentResponseDto paymentFallback(PaymentRequestDto request, Throwable throwable) {
        log.error("payment-service unavailable while charging order {}: {}", request.orderId(), throwable.getMessage());
        return new PaymentResponseDto(null, request.orderId(), request.customerId(), request.amount(),
                request.method(), "FAILED", null,
                "Payment service is temporarily unavailable. Please try again in a moment.", null);
    }
}
