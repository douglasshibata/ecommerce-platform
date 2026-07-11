package com.ecommerce.paymentservice;

import com.ecommerce.paymentservice.domain.OrderCreatedEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@CamelSpringBootTest
@TestPropertySource(properties = {
    "payment.input.endpoint=direct:test-order-events",
    "payment.output.endpoint=mock:kafka-payment-events"
})
public class PaymentServiceApplicationTests {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @BeforeEach
    public void setup() throws Exception {
        assertNotNull(camelContext);
    }

    @Test
    public void testPaymentRouteProcessing() throws Exception {
        MockEndpoint mockPaymentEvents = camelContext.getEndpoint("mock:kafka-payment-events", MockEndpoint.class);
        mockPaymentEvents.expectedMessageCount(1);

        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId("o-pay-999")
                .customerId("c-pay-777")
                .productId("p-pay-555")
                .amount(BigDecimal.valueOf(19.99))
                .quantity(1)
                .build();

        // Send a mock order created event payload as JSON to match what Kafka consumer unmarshals
        // Or simply send the object itself as Jackson can handle either depending on route unmarshalling
        producerTemplate.sendBody("direct:test-order-events", "{\"orderId\":\"o-pay-999\",\"customerId\":\"c-pay-777\",\"productId\":\"p-pay-555\",\"amount\":19.99,\"quantity\":1}");

        mockPaymentEvents.assertIsSatisfied(5, TimeUnit.SECONDS);

        String receivedJson = mockPaymentEvents.getExchanges().get(0).getIn().getBody(String.class);
        assertNotNull(receivedJson);
        org.junit.jupiter.api.Assertions.assertTrue(receivedJson.contains("o-pay-999"));
        org.junit.jupiter.api.Assertions.assertTrue(receivedJson.contains("success\":true"));
        org.junit.jupiter.api.Assertions.assertTrue(receivedJson.contains("ch_stripe_"));
    }
}
