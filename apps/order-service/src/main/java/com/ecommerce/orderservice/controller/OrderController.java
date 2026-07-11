package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderCreatedEvent;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final ProducerTemplate producerTemplate;
    private final Map<String, Order> orderRepository = new ConcurrentHashMap<>();

    public OrderController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order orderRequest) {
        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .id(orderId)
                .customerId(orderRequest.getCustomerId())
                .productId(orderRequest.getProductId())
                .quantity(orderRequest.getQuantity())
                .totalAmount(orderRequest.getTotalAmount() != null ? orderRequest.getTotalAmount() : BigDecimal.valueOf(99.99))
                .status("CREATED")
                .build();

        orderRepository.put(orderId, order);

        // Build and publish the event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .amount(order.getTotalAmount())
                .quantity(order.getQuantity())
                .build();

        // Send to camel direct endpoint which will publish to message broker (Kafka/RabbitMQ/ActiveMQ)
        producerTemplate.sendBody("direct:orderCreated", event);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        Order order = orderRepository.get(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
}
