package com.quickbite.order.client;
import com.quickbite.order.dto.external.PaymentRequestDto;
import com.quickbite.order.dto.external.PaymentResponseDto;
import com.quickbite.payment.dto.PaymentRequest;
import com.quickbite.payment.entity.PaymentMethod;
import com.quickbite.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PaymentClient {
  private final PaymentService paymentService;
  public PaymentResponseDto processPayment(PaymentRequestDto request){var r=paymentService.process(new PaymentRequest(request.orderId(),request.customerId(),request.amount(),PaymentMethod.valueOf(request.method())));return new PaymentResponseDto(r.id(),r.orderId(),r.customerId(),r.amount(),r.method().name(),r.status().name(),r.transactionRef(),r.failureReason(),r.createdAt()==null?null:r.createdAt().toString());}
  public PaymentResponseDto refund(Long orderId){var r=paymentService.refund(orderId);return new PaymentResponseDto(r.id(),r.orderId(),r.customerId(),r.amount(),r.method().name(),r.status().name(),r.transactionRef(),r.failureReason(),r.createdAt()==null?null:r.createdAt().toString());}
}