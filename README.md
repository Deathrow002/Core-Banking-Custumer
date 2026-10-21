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

This service does not log users in. Original browser routes authenticate from the **shared Redis-backed session** created by the Authentication service:

- The browser sends the `SESSION` cookie (HttpOnly, `SameSite=Lax`, raw/non-Base64 id). Spring Session loads it from Redis (namespace `core-banking:session`, 30 min timeout) and Spring Security reads the `SPRING_SECURITY_CONTEXT` attribute, so the roles (`ROLE_ADMIN` / `ROLE_MANAGER` / `ROLE_CUSTOMER`) and the `username` / `customerId` / `role` details drive `@PreAuthorize` checks.
- CSRF protection is enabled: mutating requests (`POST` / `PUT` / `DELETE`) must echo the `XSRF-TOKEN` cookie (issued by the Authentication service) in the `X-XSRF-TOKEN` header.
- Unauthenticated requests get `401 {"error":"Unauthorized"}` (no redirects). `/actuator/health`, `/actuator/info` and `/actuator/prometheus` are public.
- Browser REST requests use no request cache: a 401 never saves servlet request/cookie objects in the shared Redis session or creates a session for redirect purposes.
- Calls to other services through the load-balanced `RestTemplate` forward the caller's `Cookie` and `X-XSRF-TOKEN` headers (`SessionForwardingInterceptor`).

### Mobile OAuth2 / OIDC

All six controller methods also have aliases formed by **`/api/mobile` + the complete original absolute path**.
Thus `GET/POST/PUT /customers` becomes `GET/POST/PUT /api/mobile/customers`, and
`GET /customers/validateById?customerId=...` becomes `GET /api/mobile/customers/validateById?customerId=...`.
Aliases reuse the same handlers, business logic and role checks.

- `/api/mobile/**` is bearer-only and stateless, with a higher-priority filter chain, a null security-context
  repository and no request cache. Browser cookies cannot authenticate mobile requests and mobile tokens
  cannot authenticate original browser routes. No mobile login/logout/CSRF endpoints are exposed.
- CSRF is disabled **only** for the mobile chain. The original browser session and CSRF behavior is unchanged.
- RS256 JWT signatures, issuer, audience, expiry and not-before are validated. The `banking` scope is mandatory.
  `sub` supplies the username/email, `customerId` must be a nonempty string and `role` must exactly equal
  `ROLE_ADMIN`, `ROLE_MANAGER` or `ROLE_CUSTOMER`. Existing roles remain unchanged (Customer writes require ADMIN/MANAGER).
  `Authentication.getName()` equals `sub`; its `HashMap` details contain `username`, `customerId`, `role`.
- The native public PKCE client uses Authentication's five-minute access tokens and has no refresh-token grant.
  Acquire a new token through authorization-code + PKCE reauthorization.
- For authenticated JWT calls, `SessionForwardingInterceptor` forwards only the validated Bearer token,
  removes Cookie/CSRF headers and rewrites downstream paths to `/api/mobile` + original path exactly once,
  preserving encoded queries. Browser calls continue forwarding Cookie/CSRF headers without rewriting.

```properties
app.oauth.issuer=${OAUTH_ISSUER:http://localhost:8080}
app.oauth.jwk-set-uri=${OAUTH_JWK_SET_URI:http://authentication-service:8084/oauth2/jwks}
app.oauth.audience=core-banking-api
```

Customer REST mutations are synchronous and introduce no Kafka credential propagation. Existing asynchronous
Transaction/Loan-to-Account balance workflows depend on session ids; mobile bearer tokens must never be published
in Kafka. Mobile flows depending on that boundary require an explicit trusted authorization protocol before enqueue.

## API Endpoints

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| POST | `/customers` | Create new customer | ADMIN, MANAGER |
| GET | `/customers?customerId={uuid}` | Get customer by ID | ADMIN, MANAGER |
| GET | `/customers/all` | List all customers | ADMIN |
| GET | `/customers/validateById?customerId={uuid}` | Validate customer exists | ADMIN, MANAGER |
| GET | `/customers/validateByData` | Validate customer data | ADMIN, MANAGER |
| PUT | `/customers?customerId={uuid}` | Update customer | ADMIN, MANAGER |

The same table applies to mobile after prepending `/api/mobile` to each endpoint.

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
docker build -f Customer/Dockerfile -t customer-service .
docker run -p 8083:8083 customer-service
```

Build from the repository root: the Dockerfile copies the local root POM and Customer sources, not remote `main`.
Runtime stage and exposed port remain unchanged.

## Focused authentication tests

From the repository root, run `.\mvnw.cmd -pl Customer -Dtest=MobileSecurityTest,MobileForwardingInterceptorTest,SessionForwardingInterceptorTest,SessionContractSerializationTest test`.
These exercise the real filter chains and controller method authorization with fixed test-only RSA keys,
without an issuer, Redis, Kafka or database. HTTP tests verify bearer-only URI rewriting.