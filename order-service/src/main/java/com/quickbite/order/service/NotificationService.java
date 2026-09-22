package com.quickbite.order.service;

import com.quickbite.order.client.AuthClient;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.dto.external.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Sends order-confirmation and delivery-confirmation emails. This is
 * deliberately built to NEVER be able to break an order: it runs off the
 * request thread (@Async) and every path is wrapped so a missing/misconfigured
 * SMTP server just logs a warning instead of throwing. Out of the box
 * (`notifications.email.enabled: false`, the default) it doesn't touch the
 * network at all - it just logs what it *would* have sent, which is exactly
 * what you want while demoing without setting up a real mail account.
 *
 * To send real emails, set (see application.yml / README):
 *   NOTIFICATIONS_EMAIL_ENABLED=true
 *   MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AuthClient authClient;
    private final JavaMailSender mailSender;

    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:no-reply@quickbite.example}")
    private String fromAddress;

    @Async
    public void sendOrderConfirmation(Long customerId, OrderResponse order) {
        try {
            UserSummaryDto customer = authClient.getUser(customerId);
            String subject = "QuickBite: Order #" + order.id() + " confirmed";
            String body = "Hi " + customer.fullName() + ",\n\n"
                    + "Your order #" + order.id() + " from restaurant #" + order.restaurantId()
                    + " is confirmed and paid.\n"
                    + "Total: Rs. " + order.totalAmount() + surgeNote(order) + "\n\n"
                    + "We'll email you again once it's delivered.\n\n"
                    + "- QuickBite Delivery";
            deliver(customer.email(), subject, body, order.id());
        } catch (Exception ex) {
            logFailure(order.id(), ex);
        }
    }

    @Async
    public void sendDeliveryConfirmation(Long customerId, OrderResponse order) {
        try {
            UserSummaryDto customer = authClient.getUser(customerId);
            String subject = "QuickBite: Order #" + order.id() + " delivered";
            String body = "Hi " + customer.fullName() + ",\n\n"
                    + "Your order #" + order.id() + " has been delivered. Enjoy your meal!\n"
                    + "Total paid: Rs. " + order.totalAmount() + surgeNote(order) + "\n\n"
                    + "Rate your order any time from \"My orders\" in the app.\n\n"
                    + "- QuickBite Delivery";
            deliver(customer.email(), subject, body, order.id());
        } catch (Exception ex) {
            logFailure(order.id(), ex);
        }
    }

    private String surgeNote(OrderResponse order) {
        if (order.surgeReason() == null || order.surgeMultiplier() == null) {
            return "";
        }
        int percent = order.surgeMultiplier().subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100)).intValue();
        if (percent <= 0) {
            return "";
        }
        return " (includes a " + percent + "% surcharge: " + order.surgeReason() + ")";
    }

    private void deliver(String toAddress, String subject, String body, Long orderId) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABLED - would send] To: {} | Subject: {}\n{}", toAddress, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent '{}' email to {}", subject, toAddress);
        } catch (Exception ex) {
            // Notifications are best-effort. A down/misconfigured mail
            // server must never fail the order itself.
            logFailure(orderId, ex);
        }
    }

    private void logFailure(Long orderId, Exception ex) {
        log.warn("Could not send notification email for order {}: {}", orderId, ex.getMessage());
    }
}
