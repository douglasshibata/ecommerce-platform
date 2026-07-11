package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.domain.OrderCreatedEvent;
import com.ecommerce.paymentservice.domain.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripePaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    public OrderPaidEvent processPayment(OrderCreatedEvent event) {
        log.info("Processing Stripe charge for customer ID: {} on order ID: {} for amount: ${}",
                event.getCustomerId(), event.getOrderId(), event.getAmount());

        // Simulate Stripe API call latency & validation logic
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulating Stripe payment success conditions
        boolean isSuccess = event.getAmount() != null && event.getAmount().compareTo(BigDecimal.ZERO) > 0;
        String transactionId = isSuccess ? "ch_stripe_" + UUID.randomUUID().toString().substring(0, 12) : null;
        String errorMessage = isSuccess ? null : "Invalid amount for Stripe payment processing";

        if (isSuccess) {
            log.info("Successfully processed Stripe charge. Transaction ID: {}", transactionId);
        } else {
            log.warn("Stripe charge failed. Error message: {}", errorMessage);
        }

        return OrderPaidEvent.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getAmount())
                .success(isSuccess)
                .transactionId(transactionId)
                .errorMessage(errorMessage)
                .build();
    }
}
