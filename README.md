# POS Tunisie ☕

Cafe and restaurant point of sale for Tunisia: manager mode (French) plus customer mode.
Payments are 100% simulated (QR, Apple Pay, card, cash, infrared proximity). No real gateway.

Status: **backend done and tested**. JavaFX desktop frontend comes next.

## Stack

• ☕ Java 26
• 🌱 Spring Boot 4.1.1
• 🍃 MongoDB Atlas
• 🔑 JWT auth
• 📄 Swagger OpenAPI docs
• 📡 SSE real time orders
• 🐳 Docker ready
• 🖥️ JavaFX desktop frontend (coming next)
• 🏛️ Clean Architecture: `domain` / `application` / `infrastructure` / `presentation`

## Quickstart

```bash
cp backend-pos/.env.example backend-pos/.env   # fill MONGODB_URI + JWT_SECRET
cd backend-pos
./mvnw test                                     # 39 unit tests, no DB needed
./mvnw spring-boot:run                          # needs MongoDB reachable
```

Docker:

```bash
cd backend-pos
docker build --network=host -t pos-backend .
docker run --network=host -e MONGODB_URI=mongodb://localhost:27017/pos_tunisie pos-backend
```

Swagger UI: `http://localhost:8080/swagger-ui.html`, JSON: `/api-docs`.

## API (all under `/api/v1`)

| Area | Endpoints |
|---|---|
| Auth (manager) | `POST /api/v1/auth/login`, `/pin`, `/qr` |
| Categories | `GET/POST /api/v1/categories`, `GET/PUT/DELETE /api/v1/categories/{id}` |
| Products | `GET/POST /api/v1/products`, `GET/PUT/DELETE /api/v1/products/{id}`, `PATCH .../disponibilite` |
| Orders | `POST/GET /api/v1/orders`, `GET /api/v1/orders/{id}`, `PATCH .../statut`, `POST .../annuler` |
| Payments (simulated) | `POST /api/v1/payments/confirmer`, `POST /api/v1/payments/proximite`, `GET /api/v1/payments` |
| Tickets | `GET /api/v1/tickets`, `GET /api/v1/tickets/{id}` |
| Real time | `GET /api/v1/notifications/stream` (SSE) |

Default manager: `admin` / `admin123@`, PIN `1234`, QR key via `ADMIN_QR_KEY` (auto generated if left as default).

Paying an order marks it `PAYEE` and auto generates 2 tickets: `SERVICE` (kitchen) + `CLIENT` (receipt).

## Layout

• `backend-pos/`: Spring Boot API
• `desktop-pos/`: reserved for the JavaFX frontend (empty)
