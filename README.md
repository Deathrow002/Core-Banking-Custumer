# Core-Banking-Customer

Customer microservice for the Core Banking system. Manages customer profiles and addresses.

## Features

- Create and manage customer profiles
- Multi-address support per customer
- Customer validation
- Kafka integration for event-driven architecture
- Shared Redis-backed session authentication (SESSION cookie + CSRF) with role-based access control

## Tech Stack

- Java 17+ with Spring Boot
- PostgreSQL database
- Redis (shared Spring Session store)
- Apache Kafka for messaging
- Eureka for service discovery

## Authentication

This service does not log users in. It authenticates every request from the **shared Redis-backed session** created by the Authentication service:

- The browser sends the `SESSION` cookie (HttpOnly, `SameSite=Lax`, raw/non-Base64 id). Spring Session loads it from Redis (namespace `core-banking:session`, 30 min timeout) and Spring Security reads the `SPRING_SECURITY_CONTEXT` attribute, so the roles (`ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_USER`) and the `username` / `customerId` / `role` details drive `@PreAuthorize` checks.
- CSRF protection is enabled: mutating requests (`POST` / `PUT` / `DELETE`) must echo the `XSRF-TOKEN` cookie (issued by the Authentication service) in the `X-XSRF-TOKEN` header.
- Unauthenticated requests get `401 {"error":"Unauthorized"}` (no redirects). `/actuator/health`, `/actuator/info` and `/actuator/prometheus` are public.
- Calls to other services through the load-balanced `RestTemplate` forward the caller's `Cookie` and `X-XSRF-TOKEN` headers (`SessionForwardingInterceptor`).

## API Endpoints

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| POST | `/customers` | Create new customer | ADMIN, MANAGER |
| GET | `/customers?customerId={uuid}` | Get customer by ID | ADMIN, MANAGER |
| GET | `/customers/all` | List all customers | ADMIN |
| GET | `/customers/validateById?customerId={uuid}` | Validate customer exists | ADMIN, MANAGER |
| GET | `/customers/validateByData` | Validate customer data | ADMIN, MANAGER |
| PUT | `/customers?customerId={uuid}` | Update customer | ADMIN, MANAGER |

## Configuration

Default port: `8082`

```properties
spring.application.name=customer-service
eureka.client.service-url.defaultZone=http://discovery-service:8761/eureka
spring.data.redis.host=redis
spring.session.data.redis.namespace=core-banking:session
```

## Running Locally

```bash
./mvnw spring-boot:run
```

## Docker

```bash
docker build -t customer-service .
docker run -p 8082:8082 customer-service
```