package com.ecommerce.paymentservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private boolean success;
    private String transactionId;
    private String errorMessage;
}
