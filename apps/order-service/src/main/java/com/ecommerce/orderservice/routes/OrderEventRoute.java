package com.ecommerce.orderservice.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class OrderEventRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Core route that takes internal event trigger and publishes to downstream brokers
        from("direct:orderCreated")
            .routeId("OrderCreatedPublishRoute")
            .log("Publishing OrderCreatedEvent for order ID: ${body.orderId}")
            .marshal().json(JsonLibrary.Jackson) // Serialize event to JSON
            .to("{{order.events.endpoint:kafka:order-events?brokers=localhost:9092}}");
    }
}
