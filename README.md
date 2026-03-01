# Core-Banking-Customer

Customer microservice for the Core Banking system. Manages customer profiles and addresses.

## Features

- Create and manage customer profiles
- Multi-address support per customer
- Customer validation
- Kafka integration for event-driven architecture
- JWT authentication with role-based access control

## Tech Stack

- Java 17+ with Spring Boot
- PostgreSQL database
- Apache Kafka for messaging
- Eureka for service discovery

## API Endpoints

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| POST | `/customers` | Create new customer | ADMIN, MANAGER |
| GET | `/customers?customerId={uuid}` | Get customer by ID | ADMIN, MANAGER |
| GET | `/customers/all` | List all customers | ADMIN |
| GET | `/customers/validateById?customerId={uuid}` | Validate customer exists | ADMIN, MANAGER |
| GET | `/customers/validateByData` | Validate customer data | ADMIN, MANAGER |
| PUT | `/customers?customerId={uuid}` | Update customer | ADMIN, MANAGER |

## Request Example

## Configuration

Default port: `8082`

```properties
spring.application.name=customer-service
eureka.client.service-url.defaultZone=http://discovery-service:8761/eureka
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