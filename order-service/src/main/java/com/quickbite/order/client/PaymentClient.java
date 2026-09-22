package com.quickbite.order.client;

import com.quickbite.order.config.FeignClientConfig;
import com.quickbite.order.dto.external.PaymentRequestDto;
import com.quickbite.order.dto.external.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", configuration = FeignClientConfig.class)
public interface PaymentClient {

    @PostMapping("/payments/process")
    PaymentResponseDto processPayment(@RequestBody PaymentRequestDto request);

    @PostMapping("/payments/order/{orderId}/refund")
    PaymentResponseDto refund(@PathVariable("orderId") Long orderId);
}
