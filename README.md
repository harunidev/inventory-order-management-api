# Inventory Order Management API

A production-ready REST API built with **Spring Boot 3** for managing product inventory, customer orders, and invoices. Features JWT authentication, role-based access control, and real business logic beyond simple CRUD.

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Tech Stack](#tech-stack)
4. [Architecture](#architecture)
5. [Getting Started](#getting-started)
6. [API Documentation](#api-documentation)
7. [Authentication](#authentication)
8. [Business Rules](#business-rules)
9. [API Endpoints](#api-endpoints)
10. [Environment Variables](#environment-variables)
11. [Project Structure](#project-structure)
12. [Phase 2 Roadmap](#phase-2-roadmap)

---

## Overview

This API manages three core domains:
- **Products** — inventory with SKU tracking and stock management
- **Orders** — order lifecycle from PENDING to DELIVERED/CANCELLED, with automatic stock deduction
- **Invoices** — invoice issuance tied to orders, with payment tracking

---

## Features

### Phase 1 (Implemented)
- JWT-based authentication with role-based access (ADMIN / USER)
- Product CRUD with SKU uniqueness enforcement
- Order creation with **stock validation** (rejects if insufficient stock)
- Automatic stock deduction on order creation; stock restored on cancellation
- Invoice management with **duplicate invoice number prevention**
- Swagger/OpenAPI documentation with Bearer token support
- Docker + Docker Compose for full-stack deployment
- Global exception handling with consistent JSON error responses
- Input validation with clear field-level error messages

### Business Rules
See [Business Rules](#business-rules) section below.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Language |
| **Spring Boot 3.2.4** | Framework |
| **Spring Security** | JWT authentication & authorization |
| **Spring Data JPA** | ORM & repository layer |
| **PostgreSQL 16** | Relational database |
| **JJWT 0.12.5** | JWT token generation/validation |
| **SpringDoc OpenAPI 2.4.0** | Swagger UI |
| **Lombok** | Boilerplate reduction |
| **Docker** | Containerization |

---

## Architecture

```
src/main/java/com/harunidev/inventoryorder/
├── config/          # SecurityConfig, OpenApiConfig, ApplicationConfig
├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
├── entity/          # JPA entities: User, Product, Order, OrderItem, Invoice
├── repository/      # Spring Data JPA interfaces
├── dto/
│   ├── request/     # RegisterRequest, LoginRequest, ProductRequest, etc.
│   └── response/    # AuthResponse, ProductResponse, OrderResponse, etc.
├── service/         # Business logic: AuthService, ProductService, OrderService, InvoiceService
├── controller/      # REST endpoints: AuthController, ProductController, OrderController, InvoiceController
└── exception/       # Custom exceptions + GlobalExceptionHandler
```

**Request flow:**
```
Client → JwtAuthenticationFilter → Controller → Service (business logic) → Repository → PostgreSQL
```

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 16+ (or use Docker)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/harunidev/inventory-order-management-api.git
   cd inventory-order-management-api
   ```

2. **Set environment variables** (or create a `.env` file based on `.env.example`)
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=yourpassword
   export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   ```

3. **Create the database**
   ```sql
   CREATE DATABASE inventorydb;
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Access Swagger UI** at `http://localhost:8080/swagger-ui.html`

### Docker Setup (Recommended)

Start the full stack (PostgreSQL + API) with a single command:

```bash
# Copy env file and fill in values
cp .env.example .env

# Build and start all services
docker compose up --build
```

API will be available at `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

To stop:
```bash
docker compose down
```

To stop and remove data:
```bash
docker compose down -v
```

---

## API Documentation

Once running, full interactive API documentation is available at:

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## Authentication

The API uses **JWT Bearer Token** authentication.

### Step-by-step:

1. **Register** a new user:
   ```http
   POST /api/v1/auth/register
   Content-Type: application/json

   {
     "username": "johndoe",
     "email": "john@example.com",
     "password": "securepass123"
   }
   ```

2. **Login** to get a token:
   ```http
   POST /api/v1/auth/login
   Content-Type: application/json

   {
     "username": "johndoe",
     "password": "securepass123"
   }
   ```
   Response:
   ```json
   {
     "success": true,
     "data": {
       "token": "eyJhbGciOiJIUzI1NiJ9...",
       "tokenType": "Bearer",
       "username": "johndoe",
       "role": "USER",
       "expiresIn": 86400000
     }
   }
   ```

3. **Use the token** in subsequent requests:
   ```http
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```

4. In **Swagger UI**, click the **Authorize** button (🔓) and paste the token.

### Roles

| Role | Permissions |
|---|---|
| `USER` | Register, login, view products, create orders, view own orders, view own invoices |
| `ADMIN` | All USER permissions + manage products, view all orders, update order status, manage invoices |

To create an ADMIN user, include `"role": "ADMIN"` in the register request.

---

## Business Rules

### Rule 1: Insufficient Stock Rejection
> **"Stok 0 ise sipariş reddet"** — If stock is 0 (or less than requested quantity), the order is rejected.

When creating an order:
- Every item's requested quantity is checked against current stock **before** any stock is deducted
- If ANY item has insufficient stock, the entire order is rejected with HTTP 409
- If all checks pass, stock is deducted atomically within a single transaction
- If an order is cancelled, stock is fully restored

**Example — insufficient stock:**
```http
POST /api/v1/orders
{
  "items": [
    { "productId": 1, "quantity": 100 }
  ]
}
```
```json
{
  "success": false,
  "message": "Insufficient stock for product 'Widget A'. Requested: 100, Available: 5"
}
```

### Rule 2: Duplicate Invoice Prevention
> **"Aynı faturayı iki kez ekleme"** — The same invoice number cannot be added twice.

When creating an invoice:
- The invoice number is checked for uniqueness in the database
- If the number already exists → HTTP 409 with a clear error message
- An order can only have one invoice (one-to-one relationship)

**Example — duplicate invoice:**
```http
POST /api/v1/invoices
{ "invoiceNumber": "INV-2024-001", "orderId": 1 }
```
```json
{
  "success": false,
  "message": "Invoice number 'INV-2024-001' already exists"
}
```

---

## API Endpoints

### Authentication — `/api/v1/auth`

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/register` | Public | Register new user |
| POST | `/login` | Public | Login, receive JWT |

### Products — `/api/v1/products`

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/` | Auth | List all products |
| GET | `/{id}` | Auth | Get product by ID |
| GET | `/category/{category}` | Auth | Filter by category |
| POST | `/` | **ADMIN** | Create product |
| PUT | `/{id}` | **ADMIN** | Update product |
| PATCH | `/{id}/stock?quantity=N` | **ADMIN** | Update stock |
| DELETE | `/{id}` | **ADMIN** | Delete product |

### Orders — `/api/v1/orders`

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/` | Auth | Create order (stock check enforced) |
| GET | `/my` | Auth | Get my orders |
| GET | `/{id}` | Auth | Get order by ID |
| GET | `/` | **ADMIN** | Get all orders |
| GET | `/status/{status}` | **ADMIN** | Filter by status |
| PATCH | `/{id}/status?status=X` | **ADMIN** | Update order status |
| DELETE | `/{id}/cancel` | Auth | Cancel order |

### Invoices — `/api/v1/invoices`

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/` | **ADMIN** | Create invoice (no duplicates) |
| GET | `/` | **ADMIN** | List all invoices |
| GET | `/{id}` | **ADMIN** | Get invoice by ID |
| GET | `/number/{invoiceNumber}` | **ADMIN** | Get by invoice number |
| GET | `/order/{orderId}` | Auth | Get invoice for order |
| GET | `/unpaid` | **ADMIN** | List unpaid invoices |
| PATCH | `/{id}/pay` | **ADMIN** | Mark as paid |
| DELETE | `/{id}` | **ADMIN** | Delete invoice |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `POSTGRES_USER` | `postgres` | Docker Compose DB user |
| `POSTGRES_PASSWORD` | `postgres` | Docker Compose DB password |
| `JWT_SECRET` | (256-bit hex) | JWT signing secret (change in production!) |
| `JWT_EXPIRATION` | `86400000` | Token expiry in ms (24 hours) |

---

## Project Structure

```
inventory-order-management-api/
├── src/
│   ├── main/
│   │   ├── java/com/harunidev/inventoryorder/
│   │   │   ├── InventoryOrderManagementApiApplication.java
│   │   │   ├── config/           # Security, OpenAPI, Application config
│   │   │   ├── security/         # JWT service, filter, UserDetailsService
│   │   │   ├── entity/           # JPA entities + enums
│   │   │   ├── repository/       # JPA repositories
│   │   │   ├── dto/              # Request/Response DTOs
│   │   │   ├── service/          # Business logic layer
│   │   │   ├── controller/       # REST controllers
│   │   │   └── exception/        # Custom exceptions + global handler
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/                     # TODO: Phase 2
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── pom.xml
└── README.md
```

---

## Phase 2 Roadmap

> These features are planned for the next development phase.

### Phase 2A: Tests
- [ ] **Unit tests** — `OrderServiceTest` (stock rules), `InvoiceServiceTest` (duplicate rule), `AuthServiceTest`, `ProductServiceTest`, `JwtServiceTest`
  - Framework: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
- [ ] **Integration tests** — Full HTTP flow with real PostgreSQL via Testcontainers
  - `AuthIntegrationTest`, `OrderIntegrationTest`, `InvoiceIntegrationTest`

### Phase 2B: Advanced Features
- [ ] **Pagination & Sorting** — `Pageable` on all list endpoints
- [ ] **Low Stock Alerts** — `GET /api/v1/products/low-stock?threshold=5`
- [ ] **Pessimistic Locking** — `@Lock(PESSIMISTIC_WRITE)` for concurrent order handling
- [ ] **Refresh Tokens** — Short-lived access tokens + refresh token flow
- [ ] **Audit Logging** — `createdBy`/`modifiedBy` fields via Spring Data Auditing
- [ ] **GitHub Actions CI** — Automated test pipeline on every push
- [ ] **Flyway Migrations** — Production-safe schema versioning

---

## License

This project is licensed under the [MIT License](LICENSE).

---

*Built by [Harun Işık](https://github.com/harunidev)*
