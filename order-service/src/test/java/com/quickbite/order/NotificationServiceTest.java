package com.quickbite.order;

import com.quickbite.order.client.AuthClient;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.dto.external.UserSummaryDto;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.entity.PaymentMethod;
import com.quickbite.order.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock private AuthClient authClient;
    @Mock private JavaMailSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(authClient, mailSender);
    }

    private OrderResponse sampleOrder(BigDecimal surgeMultiplier, String surgeReason) {
        return new OrderResponse(1L, 9L, 5L, "221B Baker Street", BigDecimal.valueOf(230),
                BigDecimal.valueOf(200), surgeMultiplier, surgeReason, PaymentMethod.UPI,
                OrderStatus.PAYMENT_COMPLETED, null, null, null, Instant.now(), Instant.now(), List.of());
    }

    @Test
    void sendOrderConfirmation_doesNotEmail_whenDisabledByDefault() {
        when(authClient.getUser(9L)).thenReturn(new UserSummaryDto(9L, "Asha Rao", "asha@example.com", null, "CUSTOMER"));

        // emailEnabled defaults to false (Java's default for an unset boolean field)
        // when constructed directly rather than through Spring's @Value injection.
        notificationService.sendOrderConfirmation(9L, sampleOrder(BigDecimal.ONE, null));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderConfirmation_sendsEmail_whenEnabled() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        when(authClient.getUser(9L)).thenReturn(new UserSummaryDto(9L, "Asha Rao", "asha@example.com", null, "CUSTOMER"));

        notificationService.sendOrderConfirmation(9L, sampleOrder(new BigDecimal("1.15"), "peak dining hours"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void neverThrows_whenAuthServiceIsUnreachable() {
        when(authClient.getUser(9L)).thenThrow(new RuntimeException("auth-service unreachable"));

        // Must not propagate - a notification failure should never break an order.
        notificationService.sendOrderConfirmation(9L, sampleOrder(BigDecimal.ONE, null));
        notificationService.sendDeliveryConfirmation(9L, sampleOrder(BigDecimal.ONE, null));
    }
}
