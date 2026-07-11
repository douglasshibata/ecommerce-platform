# Event-Driven eCommerce Platform with Apache Camel & Argo CD

This repository houses a production-grade demonstration of an **event-driven microservices architecture** using **Spring Boot**, **Apache Camel**, and **Argo CD (GitOps)**.

---

## 🏗️ High-Level Architecture

The platform separates responsibilities into autonomous microservices that communicate asynchronously using a publish-subscribe topology:

```
                  ┌─────────────────┐
                  │   API Gateway   │ (HTTP REST)
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  OrderService   │ (Port 8080)
                  └────────┬────────┘
                           │ (Publish: OrderCreatedEvent)
                           ▼
                  ┌─────────────────┐
                  │   Apache Camel  │ (Enterprise Integration Patterns)
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  Message Broker │ (Kafka / RabbitMQ)
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  PaymentService │ (Port 8081)
                  └─────────────────┘
                    - Consumes: OrderCreatedEvent
                    - Integrates: Stripe API (simulated)
                    - Publishes: OrderPaidEvent
```

- **OrderService (Port 8080):** Accepts orders via HTTP REST, processes state in-memory (or PostgreSQL), and uses **Apache Camel** to serialize and dispatch an `OrderCreatedEvent` to Kafka.
- **PaymentService (Port 8081):** Consumes `OrderCreatedEvent` payloads, invokes a high-reliability **Stripe API payment gateway**, and publishes an `OrderPaidEvent` signaling payment status.

---

## 🐪 Apache Camel Integration

Each microservice leverages Apache Camel inside a Spring Boot framework to perform:
1. **Message Serialization/Deserialization:** Automatically marshalling/unmarshalling Java objects to/from JSON payloads via `camel-jackson`.
2. **Endpoint Abstraction:** Using Camel URIs (e.g., `kafka:...` or `direct:...`) to enable seamless target changes between production brokers and local test harnesses.
3. **Enterprise Integration Patterns (EIP):** Structured routes, detailed logging, dead-letter channels, and failure policies.

### Core Order Event Route

```java
from("direct:orderCreated")
    .routeId("OrderCreatedPublishRoute")
    .log("Publishing OrderCreatedEvent for order ID: ${body.orderId}")
    .marshal().json(JsonLibrary.Jackson)
    .to("{{order.events.endpoint:kafka:order-events?brokers=localhost:9092}}");
```

### Payment Processing Route

```java
from("{{payment.input.endpoint:kafka:order-events?brokers=localhost:9092}}")
    .routeId("PaymentProcessingRoute")
    .unmarshal().json(JsonLibrary.Jackson, OrderCreatedEvent.class)
    .process(exchange -> {
        OrderCreatedEvent event = exchange.getIn().getBody(OrderCreatedEvent.class);
        OrderPaidEvent responseEvent = stripeGateway.processPayment(event);
        exchange.getIn().setBody(responseEvent);
    })
    .marshal().json(JsonLibrary.Jackson)
    .to("{{payment.output.endpoint:kafka:payment-events?brokers=localhost:9092}}");
```

---

## 🤖 Argo CD & GitOps (Declarative Infrastructure)

The platform is fully containerized and uses **Kustomize** to manage Kubernetes configurations declaratively.

### Argo CD Application Definition

Argo CD monitors this repository's Kustomize manifests (`/k8s/`) and automatically syncs the live cluster with Git:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: order-service
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/ecommerce-platform.git
    targetRevision: main
    path: ecommerce-platform/apps/order-service/k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: ecommerce
  syncPolicy:
    automated:
      selfHeal: true
      prune: true
```

---

## 🚀 Getting Started (Local Development)

### 1. Prerequisites
- **Java 21 JDK**
- **Apache Maven 3.9+**
- **Docker & Docker Compose** (Optional, for deploying live broker)

### 2. Running Local Builds and Tests
Each service includes isolated unit/integration tests that mock out live message brokers to verify Camel routes and Jackson marshalling without external dependencies.

To build and run tests:

```bash
# Compile and test OrderService
cd apps/order-service
mvn clean test

# Compile and test PaymentService
cd ../payment-service
mvn clean test
```

### 3. Running Services Locally
Start up your local Kafka broker and then run each Spring Boot microservice:

```bash
# Start OrderService (Port 8080)
cd apps/order-service
mvn spring-boot:run

# Start PaymentService (Port 8081)
cd ../payment-service
mvn spring-boot:run
```

---

## 📦 CI/CD & Automated GitOps Releases

The pipeline is managed by GitHub Actions inside `.github/workflows/ci-cd.yml`:
1. **Continuous Integration (CI):** Builds Java code, executes JUnit5 unit tests, and compiles minimal, secure Docker container images.
2. **Push Registry:** Safely pushes the images to the GitHub Container Registry (GHCR) with the specific Git commit hash tag.
3. **Continuous Deployment (CD/GitOps):** Uses `kustomize edit set image` to update `kustomization.yaml` with the new tag and pushes the commit. Argo CD automatically detects the commit, synchronizes the resources, and performs a rolling update of the cluster.
