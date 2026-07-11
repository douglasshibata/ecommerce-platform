package com.ecommerce.paymentservice.routes;

import com.ecommerce.paymentservice.domain.OrderCreatedEvent;
import com.ecommerce.paymentservice.domain.OrderPaidEvent;
import com.ecommerce.paymentservice.service.StripePaymentGateway;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventRoute extends RouteBuilder {

    private final StripePaymentGateway stripeGateway;

    public PaymentEventRoute(StripePaymentGateway stripeGateway) {
        this.stripeGateway = stripeGateway;
    }

    @Override
    public void configure() throws Exception {
        // Consumes from Kafka, processes with Stripe gateway, then publishes response
        from("{{payment.input.endpoint:kafka:order-events?brokers=localhost:9092}}")
            .routeId("PaymentProcessingRoute")
            .log("Received order-events message. Processing payment...")
            .unmarshal().json(JsonLibrary.Jackson, OrderCreatedEvent.class)
            .process(exchange -> {
                OrderCreatedEvent event = exchange.getIn().getBody(OrderCreatedEvent.class);
                OrderPaidEvent responseEvent = stripeGateway.processPayment(event);
                exchange.getIn().setBody(responseEvent);
            })
            .log("Payment process outcome: order ID: ${body.orderId}, success: ${body.success}")
            .marshal().json(JsonLibrary.Jackson)
            .to("{{payment.output.endpoint:kafka:payment-events?brokers=localhost:9092}}");
    }
}
