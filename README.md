# 🇹🇳 POS Tunisie — Café & Restaurant

> A point-of-sale platform designed for Tunisian cafés and restaurants: fast order taking, simulated payment, kitchen-ready tickets, and real-time order visibility.

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,mongodb,maven,javafx,docker,git,github" alt="Technology stack: Java, Spring, MongoDB, Maven, JavaFX, Docker, Git, GitHub" />
</p>

## Product at a glance

<p align="center">
  <img src="docs/screenshots/image.png" alt="POS Tunisie home screen" width="49%" />
  <img src="docs/screenshots/image2.png" alt="Customer menu with products and basket" width="49%" />
</p>

<p align="center">
  <img src="docs/screenshots/image3.png" alt="Customer order basket" width="49%" />
  <img src="docs/screenshots/image4.png" alt="Payment methods screen" width="49%" />
</p>

<p align="center">
  <img src="docs/screenshots/image5.png" alt="Kitchen and customer ticket preview" width="49%" />
  <img src="docs/screenshots/image6.png" alt="Manager password login" width="49%" />
</p>

<p align="center">
  <img src="docs/screenshots/image7.png" alt="Manager PIN login" width="49%" />
  <img src="docs/screenshots/image8.png" alt="Manager operations dashboard" width="49%" />
</p>

<p align="center">
  <img src="docs/screenshots/image9.png" alt="Manager catalog administration" width="49%" />
</p>



## Project snapshot

| Item | Current state |
| --- | --- |
| Product scope | Café and restaurant point of sale |
| Primary users | Manager, cashier, kitchen/service team, customer |
| Delivery status | Backend API and JavaFX desktop client implemented and tested |
| Language | French-oriented workflows and Tunisia-specific configuration |
| Payments | Fully simulated — no real payment gateway is connected |

## Why this product

POS Tunisie gives an establishment one operational flow from catalog to receipt:

1. The team manages categories and available products.
2. A cashier creates an order using a price and product-name snapshot.
3. Payment is confirmed through a simulated method.
4. The order becomes `PAYEE` and the system automatically produces exactly two 58 mm tickets: one for service/kitchen and one customer receipt.
5. Connected screens receive order updates in real time through Server-Sent Events (SSE).

## Business capabilities

| Capability | What it supports |
| --- | --- |
| Catalog management | Categories, products, prices, and availability |
| Order management | Create, view, update status, and cancel orders |
| Authentication | Manager sign-in by password, PIN, or QR key; JWT-protected management actions |
| Payments | Simulated cash, card, QR, Apple Pay, infrared proximity, and other configured methods |
| Tickets | Kitchen/service ticket plus customer receipt after payment |
| Real-time operations | Live order notifications over SSE |
| API documentation | Interactive Swagger/OpenAPI documentation |

## Desktop application

The `desktop-pos/` module is the JavaFX client for customer self-service and manager operations. It connects to the Spring Boot API over REST and receives live order updates through SSE.

### Main workflows

- **Customer mode:** browse the café catalog, add items to a basket, review the order, and confirm a simulated payment using cash, card, QR, NFC/proximity, or configured digital methods.
- **Manager mode:** sign in with a password, PIN, or manager QR badge; monitor live orders; manage categories and products; review tickets; and change application settings.
- **Tickets:** the backend generates a service/kitchen ticket and a customer receipt after simulated payment confirmation.
- **Interface:** English and French-oriented labels, light/dark theme switching, responsive JavaFX layouts, Tunisian café imagery, QR support, and 58 mm ticket previews.

### Desktop verification

Run the desktop tests from the module directory:

```bash
cd desktop-pos
./mvnw test
```

Verification status: 19 tests passed, including UI parsing, headless UI smoke coverage, Arabic layout safety, QR/proximity behavior, and localization/text safety.



## Delivery scope

### Available now

- Spring Boot backend API following Clean Architecture
- MongoDB persistence adapters
- JWT authentication and manager seed account
- Simulated payment confirmation and 58 mm ticket rendering
- SSE order-notification stream
- Swagger UI and Docker build definition
- Unit-test suite that runs without a database

### Desktop client

- JavaFX customer and manager application in `desktop-pos/`
- REST and SSE integration with the backend
- Catalog, basket, payment, ticket, authentication, QR, NFC/proximity, and settings views
- Reference imagery in `desktop-pos/assets/`

## Technical foundation

| Layer | Technology / approach |
| --- | --- |
| Runtime | Java 26 |
| Backend | Spring Boot 4.1.1 (Spring MVC) |
| Database | MongoDB / MongoDB Atlas |
| Security | JWT with password, PIN, and QR login options |
| Documentation | springdoc OpenAPI / Swagger UI |
| Real-time | Server-Sent Events |
| Architecture | Clean Architecture: `domain` → `application` → `infrastructure` → `presentation` |
| Packaging | Docker |

## Repository structure

```text
backend-pos/     Spring Boot API
desktop-pos/     JavaFX desktop client
  assets/        Tunisian cuisine and café reference imagery for the future UI
docs/screenshots/ Live desktop application screenshots
LICENSE          Custom non-commercial license
```

## Run locally

### Prerequisites

- JDK 26
- MongoDB instance reachable by the backend (required only to run the application)
- Docker, if using the container option

### Setup and test

```bash
cp backend-pos/.env.example backend-pos/.env
# Update MONGODB_URI and JWT_SECRET in backend-pos/.env

cd backend-pos
./mvnw test
./mvnw spring-boot:run
```

In a second terminal, run the desktop client:

```bash
cd desktop-pos
./mvnw javafx:run -Dpos.apiBase=http://localhost:8080/api/v1
```

Or start both services together from the repository root:

```bash
./run-all.sh
```

The backend unit tests mock MongoDB and therefore do not require a running database. The desktop tests use headless JavaFX smoke checks and do not require a display. Environment files are loaded from `backend-pos/.env` by the configured application startup support.

## Install on another computer

GitHub Actions builds native installers for the desktop client:

- Linux: `.deb`
- Windows: `.exe`
- macOS: `.dmg`

To build them, open the **Build POS installers** workflow in GitHub Actions and choose **Run workflow**. For a permanent download, create a version tag such as `v0.1.0`; the workflow attaches the three installers to the GitHub release. The workflow also keeps each installer available as a downloadable artifact.

The installer is self-contained for local testing: it includes the JavaFX desktop application, the Spring Boot API, and a local MongoDB server. On launch it starts MongoDB with a persistent data directory at `~/.pos-tunisie/mongodb-data`, starts the API on `127.0.0.1:18080`, and opens the desktop client. Your Atlas URI and repository `.env` are never included in the installer. The first local manager account uses `admin` / `admin123@` and PIN `1234`; change these before shared or production use.

### Run with Docker

```bash
cd backend-pos
docker build --network=host -t pos-backend .
docker run --network=host \
  -e MONGODB_URI=mongodb://localhost:27017/pos_tunisie \
  pos-backend
```

## API access

Once the application is running:

| Resource | Address |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api-docs` |
| API base path | `http://localhost:8080/api/v1` |
| Live notifications | `GET /api/v1/notifications/stream` |

### API map

| Area | Endpoints |
| --- | --- |
| Authentication | `POST /auth/login`, `/auth/pin`, `/auth/qr` |
| Categories | `GET/POST /categories`, `GET/PUT/DELETE /categories/{id}`, `GET /categories/actives` |
| Products | `GET/POST /products`, `GET/PUT/DELETE /products/{id}`, `PATCH /products/{id}/disponibilite` |
| Orders | `POST/GET /orders`, `GET /orders/{id}`, `PATCH /orders/{id}/statut`, `POST /orders/{id}/annuler` |
| Payments (simulated) | `POST /payments/confirmer`, `POST /payments/proximite`, `GET /payments` |
| Tickets | `GET /tickets`, `GET /tickets/{id}` |

All paths in the table above are relative to `/api/v1`. Catalog reads, order creation, payment endpoints, authentication, live notifications, and API documentation are publicly available. Management writes require a bearer JWT.

## Demo manager account

| Method | Default |
| --- | --- |
| Username / password | `admin` / `admin123@` |
| PIN | `1234` |
| QR key | Configure `ADMIN_QR_KEY`; a key is generated when the default placeholder remains |

Change all default credentials and the JWT secret before any shared or production deployment. Do not commit `backend-pos/.env`.


## Important implementation note

Payments are deliberately simulated. This project must not be connected to a real payment gateway without a separate, security-reviewed payment integration scope.

## License and usage

This project is available under the custom [Non-Commercial License](LICENSE). Developers may copy, study, modify, and use the software for personal, educational, or non-commercial purposes. Commercial use, resale, sublicensing, paid hosting, and incorporating the software into a paid product are not permitted without written permission from Mohamed Amine Ammar, the rights holder.

For commercial licensing or permission, contact Mohamed Amine Ammar through the project repository.
