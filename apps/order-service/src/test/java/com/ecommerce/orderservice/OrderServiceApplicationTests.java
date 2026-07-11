package com.ecommerce.orderservice;

import com.ecommerce.orderservice.domain.OrderCreatedEvent;
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
    // Override the endpoint to point to a local mock for unit testing
    "order.events.endpoint=mock:kafka-order-events"
})
public class OrderServiceApplicationTests {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @BeforeEach
    public void setup() throws Exception {
        // Ensure Camel context is running
        assertNotNull(camelContext);
    }

    @Test
    public void testOrderCreatedRoute() throws Exception {
        // Obtain our mock endpoint
        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-order-events", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        // Build sample order created event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("o-test-123")
                .customerId("c-test-456")
                .productId("p-test-789")
                .amount(BigDecimal.valueOf(149.99))
                .quantity(2)
                .build();

        // Send event to the starting endpoint
        producerTemplate.sendBody("direct:orderCreated", event);

        // Assertions
        mockKafka.assertIsSatisfied(5, TimeUnit.SECONDS);

        String receivedJson = mockKafka.getExchanges().get(0).getIn().getBody(String.class);
        assertNotNull(receivedJson);
        org.junit.jupiter.api.Assertions.assertTrue(receivedJson.contains("o-test-123"));
        org.junit.jupiter.api.Assertions.assertTrue(receivedJson.contains("c-test-456"));
    }
}
