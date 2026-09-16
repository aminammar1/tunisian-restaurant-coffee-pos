# AGENTS.md — tunisian-restaurant-coffee-pos

## Layout
- `backend-pos/` — Spring Boot API (only implemented part). Package `tn.cafe.pos`.
- `desktop-pos/` — empty, reserved for future JavaFX frontend. Do not scaffold until asked.
- No README, no CI. Only `.gitignore` files are root `.gitignore` + `backend-pos/.gitignore`.

## Backend stack (verified in `backend-pos/pom.xml`)
- Spring Boot `4.1.1`, Java `26`, MongoDB, JJWT `0.12.6`, springdoc `3.1.0`.
- Boot 4 notes: web starter is `spring-boot-starter-webmvc` (not `starter-web`); springdoc artifact is `springdoc-openapi-starter-webmvc-ui` v3 (v2 only works on Boot 3).
- Clean Architecture: `domain/` = pure POJOs + repository interfaces + exceptions (no Spring/Mongo annotations); `application/` = `dto/` records + `service/`; `infrastructure/` = `persistence/{document,mongo,adapter,mapper}`, `security/`, `config/`, `realtime/`, `printing/`, `qr/`; `presentation/` = `controller/` + `advice/`.
- Domain docs live in `infrastructure/persistence/document/`; mapping is manual in `Mappers.java` (no MapStruct). Adapters in `.../adapter/` implement `domain/repository/` interfaces.

## Build / test (no system `mvn` or `gradle` on PATH)
- Always run from `backend-pos/`: `./mvnw test`, `./mvnw -Dtest=AuthServiceTest test`.
- First run downloads Maven wrapper `3.9.16`; requires network + JDK 26 (`/usr/lib/jvm/java-26-openjdk`).
- 39 unit tests (JUnit 5 + Mockito), all mock `Spring*Mongo` — no DB needed. Mockito self-attach / byte-buddy warnings on JDK 26 are expected noise.
- `BackendPosApplicationTests` is intentionally NOT `@SpringBootTest` (avoids needing Mongo). Do not re-add `@SpringBootTest` without a test DB.

## Env / config gotchas
- `src/main/resources/application.properties` reads env vars with local defaults; Spring does NOT load `.env` (no dotenv lib). Export vars in shell or pass `-Dspring.data.mongodb.uri=...` when running tests/app.
- Template: `backend-pos/.env.example`. Real `backend-pos/.env` holds an Atlas URI + a generated JWT secret.
- `.env` (Atlas URI + JWT secret) is git-ignored at root and in `backend-pos/`. Never force-add it.
- Atlas DB name is the URI path segment (`...mongodb.net/pos_tunisie?...`) plus `MONGODB_DATABASE=pos_tunisie`.

## Rules you will otherwise break
- Payments are 100% simulated — never add a real gateway. `PaymentService.confirmer()` must mark order `PAYEE` and call `TicketService.genererDeuxTickets()` (exactly 2: `SERVICE` cuisine + `CLIENT` receipt, rendered by `TicketRenderer` 58mm text).
- `OrderService.creer()` snapshots product name/price into `OrderItem`s and rejects unavailable products; it publishes to `OrderSseHub` (SSE `GET /api/notifications/stream`).
- Auth: `AuthService` supports password (`admin`/`admin123@` defaults), PIN (`1234` default), and QR key (`ADMIN_QR_KEY`, auto-generated if `CHANGE-ME`). `AdminSeeder` creates the gérant once on boot. `SecurityConfig` leaves catalog reads, `POST /api/orders`, `POST /api/payments/**`, auth, SSE, and swagger public; all other writes need `Bearer` JWT.
- Swagger UI at `/swagger-ui.html`, JSON at `/api-docs`.
