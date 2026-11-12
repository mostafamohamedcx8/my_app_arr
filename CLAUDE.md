# CLAUDE.md — Eqdeha Backend

## What this project is
**Eqdeha** ("get it done") is a local errand/delivery app, launching in one area: **Ezbet Sekina** (Egypt).
A **customer** orders items; a **courier** from the area buys them and delivers them.

- The app **never takes money from customers.** Customers pay the courier directly (cash, or transfer to the courier's wallet number).
- The app's only revenue is a **commission on the delivery fee**, charged to the courier and accumulated as a debt the courier pays back.
- Three clients use this backend: **customer app**, **courier app** (mobile, Arabic RTL UI), and an **admin web dashboard**.

> Stack: Java 21 · Spring Boot 4.1.2 · MySQL · Flyway · Maven. Hosting: `[fill in]`.

## Tech stack & versions
Versions are managed by the Spring Boot parent BOM — **do not pin library versions in `pom.xml`** unless overriding the BOM on purpose.

| Tool | Version | Notes |
|---|---|---|
| Java (JDK) | 21 (LTS) | `java.version` in pom |
| Spring Boot | 4.1.2 | `spring-boot-starter-parent` |
| Spring Framework (core, WebMVC) | 7.0.10 | `spring-boot-starter-webmvc` |
| Spring Security | 7.1.2 | `spring-boot-starter-security` |
| Spring Data JPA / Hibernate ORM | Hibernate 7.4.13.Final | `ddl-auto=validate` — Hibernate never changes the schema |
| Hibernate Validator (Jakarta Validation) | 9.1.5.Final | `spring-boot-starter-validation` |
| Flyway (core + mysql) | 12.4.0 | owns the schema; migrations in `src/main/resources/db/migration` |
| MySQL Connector/J | 9.7.0 | runtime — the only JDBC driver, all profiles (`com.mysql.cj.jdbc.Driver`) |
| HikariCP | 7.0.2 | connection pool |
| Jackson | 3.1.7 | package is `tools.jackson.*` (Jackson 3), not `com.fasterxml.jackson.*` |
| Embedded Tomcat | 11.0.27 | |
| Lombok | 1.18.48 | annotation processor configured in pom |
| Maven | 3.10.0 via wrapper | always use `./mvnw` (`mvnw.cmd` on Windows), not a global `mvn` |
| JUnit Jupiter / Mockito | 6.0.3 / 5.23.0 | from the Boot test starters |
| Database (local) | **MariaDB 12.3** (XAMPP) on `127.0.0.1:3306`, schema `myapp`, user `root` | utf8mb4 (Arabic text) |
| Database (dev/prod) | MySQL `[fill in version]` | |

Base package: `com.myapp.aqdeeha` · artifact `aqdeeha` · main class `AqdeehaApplication`.

## Commands
```bash
./mvnw spring-boot:run                                   # runs with the default 'local' profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev    # another profile
./mvnw test                                              # all tests
./mvnw test -Dtest=ClassName#method                      # single test
./mvnw clean package                                     # build jar → target/
java -jar target/aqdeeha-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Configuration & profiles
All config is YAML in `src/main/resources/`:

| File | Purpose |
|---|---|
| `application.yml` | shared settings for every profile (JPA, Flyway, Jackson, UTC, i18n, multipart). Sets `local` as the default profile |
| `application-local.yml` | developer machine. **Git-ignored** (holds local DB password). Copy from `application-local.yml.example` |
| `application-dev.yml` | shared dev/staging server. Values from env vars only |
| `application-prod.yml` | production. Values from env vars only, SSL required to DB, minimal logging/error detail |

- Select a profile with `SPRING_PROFILES_ACTIVE=local|dev|prod`.
- Env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`, `SERVER_PORT`, `JWT_SECRET`, `HASH_PEPPER`, `BOOTSTRAP_KEY`, `ID_CARD_ENCRYPTION_KEY` (never change once ID cards exist), `STORAGE_ROOT`, `WHATSAPP_PROVIDER`, `GOOGLE_MAPS_API_KEY` (optional; empty → all distances ESTIMATED), `MAPS_PROVIDER` (default `google`), `CORS_ALLOWED_ORIGINS` (browser origins, comma-separated; local default allows the Expo web dev server on :8081) — see `.env.example`. `application-local.yml` has local defaults for all of them.
- Never put secrets in `application.yml`, `application-dev.yml`, or `application-prod.yml`.
- Business values (fees, commission, limits, points, service area) are **not** YAML config — they live in the `application_config` table (see below).

## Database & migrations
- Flyway owns the schema. Every schema change = a new file `V<n>__<description>.sql` in `src/main/resources/db/migration` (e.g. `V1__create_users.sql`). **Never edit a migration that has already been applied**; add a new one.
- Tables use `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`.
- `spring.flyway.clean-disabled=true` everywhere — never drop the DB through Flyway.
- The app always connects with the MySQL driver (`jdbc:mysql://`). The local server is MariaDB (XAMPP), so `application-local.yml` sets `spring.jpa.database-platform: org.hibernate.dialect.MariaDBDialect` explicitly — Connector/J 9.x can't read MariaDB's keyword metadata, so Hibernate can't auto-detect it. The startup warnings `HHH100045 ... Unknown column 'RESERVED'` and `HHH90000025` are expected locally and harmless. Write migrations that run on both MariaDB and MySQL.
- `spring.jpa.open-in-view=false`: load everything you need inside the service transaction.

## Project structure (package-by-feature)
```
src/main/java/com/myapp/aqdeeha/
├── AqdeehaApplication.java
├── config/            # Spring config: security chains, Jackson, CORS, Clock
├── security/          # JWT, current-user resolution, role checks
├── common/            # shared, depends on no feature module
│   ├── api/           # response/error DTOs
│   ├── exception/     # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── entity/        # BaseEntity / auditing superclasses
│   ├── i18n/          # Messages (Arabic text lookup)
│   ├── storage/       # FileStorageService, ImageValidator
│   └── util/          # PhoneNumbers, PasswordPolicy, LowercaseEnumConverter
└── modules/           # one package per feature, owning its tables
    ├── auth/          # register/login/OTP/reset for all roles (otp_codes)
    ├── user/          # users, device_tokens
    ├── courier/       # courier_profiles
    ├── address/       # addresses
    ├── admin/         # admins
    ├── catalog/       # categories, shops
    ├── order/         # orders, order_stops, order_offers, order_status_history
    ├── invoice/       # invoices, invoice_items
    ├── chat/          # messages
    ├── rating/        # ratings
    ├── notification/  # notifications
    ├── finance/       # courier_ledger, commission_payments
    ├── points/        # points_transactions, points_redemptions
    ├── moderation/    # complaints, bans
    ├── setting/       # application_config (ApplicationConfigService, ConfigKeys)
    └── audit/         # audit_log
src/main/resources/
├── application*.yml
├── db/migration/      # Flyway SQL
└── i18n/messages.properties   # error code → Egyptian Arabic message
```

Inside each module, use these sub-packages (create them as needed):
```
modules/<feature>/
├── controller/   # @RestController — thin: validate input, call service, map to DTO
├── service/      # business rules + @Transactional boundaries
├── repository/   # Spring Data JPA interfaces
├── entity/       # JPA entities
├── dto/          # request/response records (Jakarta Validation annotations here)
└── mapper/       # entity ↔ DTO
```
Rules:
- Controllers never touch repositories directly; never return entities from controllers — return DTOs.
- A module calls another module only through its **service**, never its repository.
- Admin endpoints under `/api/admin/**`; mobile endpoints under `/api/v1/**`.
- **Access control — never check roles by hand in controllers/services.** Annotate every `/api` controller method (or class):
  - `@PublicEndpoint` — no token needed (login, signup, OTP).
  - `@Protect` — any fully active account (not the limited `COURIER_APPLICANT`).
  - `@AllowTo(ADMIN)` / `@AllowTo({CUSTOMER, COURIER})` … — listed roles only. `SUPER_ADMIN` is implied wherever `ADMIN` is listed. Method-level annotation overrides class-level.
  - Enforced once by method security (`security/access/AccessRuleAuthorizationManager`, wired in `config/MethodSecurityConfig`). No token → 401 `TOKEN_INVALID`, wrong role → 403 `FORBIDDEN`.
  - `AccessAnnotationVerifier` **fails startup** if an `/api` endpoint has none of the three.
  - Current account: `CurrentUser.get()` / `CurrentUser.id()` (never `@AuthenticationPrincipal`).
- Paginated lists return `PageResponse` `{items, page, size, totalItems, totalPages, hasNext}`; query params `page` (0-based), `size` (default 20, max 100), `q` for search. Sorting is fixed per query.
- Admin actions are audited with `AuditService.record(...)` inside the same transaction (it requires one).
- Tests mirror the main package layout under `src/test/java`. Integration tests extend `support/IntegrationTestBase` (`@ActiveProfiles({"local","test"})`): they run against a **separate DB `myapp_test`** (auto-created), truncate all tables before each test, and capture OTPs with `RecordingWhatsAppSender`. Add new tables to `IntegrationTestBase.TABLES`.
- Enums are stored and returned as lower-case strings (`super_admin`) via a `LowercaseEnumConverter` subclass per enum.
- Validation messages are keys (`message = "validation.required"`), resolved from `messages.properties` by `GlobalExceptionHandler`.
- Error body: `{ "code", "message", "fieldErrors": [{ "field", "message" }], "timestamp" }`.

## API — implemented (Phase 1: auth)
| Method | Path | Auth |
|---|---|---|
| POST | `/api/v1/auth/customer/signup/send-otp` `{phone, acceptTerms, termsVersion}` · `/verify-otp` · `/complete` (multipart) | public |
| POST | `/api/v1/auth/courier/signup/send-otp` `{phone, acceptTerms, termsVersion}` · `/verify-otp` · `/complete` (multipart) | public |
| POST | `/api/v1/auth/login` | public |
| POST | `/api/v1/auth/password/forgot` · `/password/verify-otp` · `/password/reset` | public |
| GET  | `/api/v1/auth/me` | customer / courier |
| POST | `/api/admin/auth/login` | public |
| POST | `/api/admin/auth/bootstrap-super-admin` (header `X-Bootstrap-Key`) | public, one-time |
| GET  | `/api/admin/auth/me` | admin / super_admin |
| POST | `/api/admin/admins` | super_admin |
| GET  | `/api/admin/couriers?status=&q=&page=&size=` | admin |
| GET  | `/api/admin/couriers/{id}` (includes protected ID-card URLs) | admin |
| GET  | `/api/admin/couriers/{id}/id-card/{front\|back}` (decrypted image, `no-store`) | admin |
| PATCH | `/api/admin/couriers/{id}/approval` `{status: approved\|rejected\|needs_fix, note, fields[]}` (note required unless approved; fields required for needs_fix; audited) | admin |
| GET  | `/api/v1/courier/me` (includes `nextStep`) | courier, courier_applicant |
| POST | `/api/admin/categories` (multipart) · GET `/api/admin/categories[/{id}]` · PATCH `/{id}` (multipart, partial) · DELETE `/{id}` | admin (audited) |
| GET  | `/api/admin/shops?categoryId=&q=` · GET/PATCH (multipart)/DELETE `/api/admin/shops/{id}` | admin (audited) |
| GET  | `/api/files/id-cards/{courierId}/{side}?a=&exp=&sig=` (signed link, no token) | public, signature-checked |
| POST | `/api/v1/auth/courier/fix/send-otp` · `/api/v1/auth/courier/fix/verify-otp` (phone + code → limited token) | public |
| GET  | `/api/v1/courier/application` (only the flagged fields) · POST `/api/v1/courier/application/resubmit` (multipart) | courier_applicant |
| PUT  | `/api/v1/courier/wallet` | courier |
| GET  | `/api/v1/notifications` · POST `/api/v1/notifications/{id}/read` | customer, courier, courier_applicant |
| GET  | `/api/v1/categories` (active, by sortOrder) | any logged-in |
| POST | `/api/admin/shops` (multipart, audited) | admin |
| GET  | `/api/v1/shops?categoryId=&q=&page=&size=` · `/api/v1/shops/{id}` (active categories only) | any logged-in |
| GET  | `/api/admin/config` | admin |
| PUT  | `/api/admin/config/{key}` `{value}` (audited) | super_admin |
| GET  | `/api/v1/terms?audience=CUSTOMER\|COURIER` → `{id, audience, version, content, publishedAt}` | public |
| POST | `/api/v1/terms/accept` `{termsVersionId}` (current version of the caller's audience only, idempotent) | customer, courier, courier_applicant |
| GET  | `/api/admin/terms?audience=` (all versions + acceptanceCount) | admin |
| POST | `/api/admin/terms` `{audience, content}` (new version = last + 1, becomes current, audited `terms.published`) | super_admin |
| POST | `/api/v1/orders/quote` `{type: shops\|anything, shopIds[], dropoffLat, dropoffLng, pickupLat?, pickupLng?}` → `{priceType, deliveryFee, minFee, maxFee, distanceKm, distanceSource, shopCount}` | customer |

Postman collection: `postman/Aqdeeha.postman_collection.json`.

## Roles
| Role | Table | Login |
|---|---|---|
| customer | `users` (role=customer) | phone + password. WhatsApp OTP verifies the phone at signup and is used for password reset |
| courier | `users` (role=courier) + `courier_profiles` | phone + password (same OTP flow), then **admin approval** before working |
| admin | `admins` (role=admin) | **email** + password. Created only by the super_admin (`POST /api/admin/admins`). Admin password reset: not built yet |
| super_admin | `admins` (role=super_admin) | **email** + password. **Exactly one** in the system (DB-enforced). Created once via `POST /api/admin/auth/bootstrap-super-admin` (needs `X-Bootstrap-Key` = `BOOTSTRAP_KEY`); fails forever after. Only role that can create admins and change `application_config` (commission, fees…) |

### Auth rules
- JWT (HS256) valid **30 days**. Claims: `iss`, `sub` (`user:<id>` / `admin:<id>`), `role`, `jti`, `iat`, `exp`. The HMAC hash of every issued token is stored in `auth_tokens`; a request is accepted only if the signature is valid **and** the row exists, is not revoked/expired, **and** the account is still `active` (users) / `is_active` (admins). A password reset revokes all of the user's tokens.
- Passwords: **min 8 chars, at least one letter, one digit and one special character**. Hashed with BCrypt (strength 12). Never logged or returned.
- OTP: 6 digits, stored as HMAC-SHA256 (with `HASH_PEPPER`), 5-minute expiry, max 5 wrong attempts, resend once per 60 s, max 5 per phone and 20 per IP per hour. Purposes: `signup`, `reset_password`, `courier_fix`. A correct code returns a one-time `verificationToken` (15 min) that the next step must send.
- **WhatsApp carries ONLY verification codes** (signup, courier fix access, password recovery). `WhatsAppSender` has just `sendOtp` — never add free-text messages. Every other notification is stored in `notifications` only (real-time delivery via WebSockets later). Only `ConsoleWhatsAppSender` exists (`app.whatsapp.provider=console`), which **prints the code in the log** — dev only.
- Images: jpg/png/webp only (checked by magic bytes), max 5 MB. Public photos live under `uploads/public` and are served at `/files/public/**`. ID-card images are AES-256-GCM encrypted (`ID_CARD_ENCRYPTION_KEY`) under `uploads/private`, never served and never returned by any API.
- Courier signup creates `users.status=pending` + `approval_status=pending`; login is refused until both are approved/active.
- Approval (`PATCH /api/admin/couriers/{id}/approval`): `approved` → account `pending→active`; `rejected`/`needs_fix` → account `active→pending`; leaving `approved` revokes all courier tokens. Suspended/banned accounts keep their status. Every decision → `audit_log` **and** an in-app notification (DB only, no WhatsApp).
- Courier lifecycle & login `nextStep`:
  - `pending` / `rejected` → login 403 (`COURIER_PENDING_APPROVAL` / `COURIER_REJECTED`).
  - `needs_fix`: the admin **must** send `fields` (subset of `FixField`: fullName, birthDate, photo, address, vehicleType, vehicleColor, plateNumber, vehiclePhoto, idCardFront, idCardBack, wallet), stored in `courier_profiles.fix_fields`.
  - The courier opens it with **phone + WhatsApp code** (`/api/v1/auth/courier/fix/send-otp` → `/verify-otp`, OTP purpose `courier_fix`) or phone + password login. Both return `nextStep: "fix_application"` and a token that resolves to role `COURIER_APPLICANT` (derived per request, never stored). It reaches only: `GET /api/v1/courier/application` (ONLY the flagged fields with label/inputType/formKeys/currentValue), `POST /api/v1/courier/application/resubmit`, `GET /api/v1/courier/me`, notifications. `@Protect` does NOT accept it.
  - Resubmit: every flagged field is required (`FieldErrorsException` lists the missing ones), unflagged fields are ignored → back to `pending`, `fix_fields` cleared, `resubmission_count+1`, tokens revoked.
  - Wallet (`walletType` + `walletNumber`) is **required at courier signup**. `nextStep: "add_wallet"` exists only for old accounts without one. Change later with `PUT /api/v1/courier/wallet`.
- Wallet: `vodafone_cash` = Egyptian mobile stored as `01XXXXXXXXX`; `instapay` = mobile (same form) or `name@instapay` (lower-case).
- ID cards for the dashboard: courier details return **signed links** (`/api/files/id-cards/{id}/{side}?a=&exp=&sig=`, HMAC, bound to the requesting admin, valid `app.storage.signed-url-ttl` = 5 min) usable in `<img src>` without a token, plus token-protected download URLs. Both responses are `no-store`.
- Phones: Egyptian mobiles only, normalized to `+201XXXXXXXXX`.
- Plate number (optional at signup; resubmit when flagged): letters (Arabic/Latin) and/or digits (Arabic-Indic/Latin) in any order, spaces allowed between them, **max 8 letters + digits** (spaces not counted). `common/util/PlateNumbers` (`REGEX` for `@Pattern`, `normalize` = trim + single spaces before saving). Error `validation.plate_number`.
- **Terms & conditions** (`modules/terms`, `TermsService`):
  - Versioned per audience (`CUSTOMER` / `COURIER`). Exactly one current version per audience (DB guard). Text, number and audience of a published version never change.
  - Signup step 1 must send `acceptTerms: true` **and** `termsVersion` = the current version the app showed (`GET /api/v1/terms`); otherwise 400 `TERMS_NOT_ACCEPTED`. The accepted version id + IP + user agent are kept on the OTP row, and step 3 writes the `terms_acceptances` row in the same transaction as the account.
  - Login and `GET /me` return `user.termsUpToDate` (`/api/v1/courier/me`: `termsUpToDate`). `false` → the app shows the new text and calls `POST /api/v1/terms/accept`. Out-of-date terms don't block the API (yet).
  - `terms_acceptances` is a legal record: append-only (Hibernate `@Immutable`, repository without delete, **DB triggers reject UPDATE/DELETE**). Never remove those triggers.
  - Content format: plain Arabic text; a line starting with `## ` is a section title.
  - Users created before V15 got a backfilled acceptance of version 1 (`user_agent` = `backfill: …`).
- Login never reveals whether an account exists (`INVALID_CREDENTIALS` for both cases). Forgot-password **does**, on purpose (product decision): unknown phone → 404 `PHONE_NOT_REGISTERED`; signup step 1 returns `PHONE_ALREADY_REGISTERED`.

## Glossary (product term → code)
| Product term | Code |
|---|---|
| Order from shops | order `type=shops` |
| Order anything (from anywhere / from inside Sekina) | order `type=anything`, `scope=anywhere / inside` |
| Shop invoice | `invoices` (one per shop stop) |
| Final invoice | computed: sum(invoices.total) + delivery_fee — **no table** |
| Pickup | status `picked_up` (courier has collected all items) |
| Commission | `commission_amount` on order, recorded in `courier_ledger` |
| Courier debt (owed to the app) | `courier_profiles.debt` |
| Advance | 50 EGP credit granted after a commission payment proof upload |
| Points | `users.points`, history in `points_transactions` |
| Categories | `categories` |

## Data model (summary)
Core tables — keep names exactly as below.

- **users**: id, role (`customer|courier`), phone (unique, `+201…`), password_hash, phone_verified_at, terms_accepted_at, name, email (unique, nullable), birth_date, gender (`male|female`, nullable for couriers), photo_url (storage key), status (`active|pending|suspended|banned`), points, password_changed_at, last_login_at, created_at, updated_at
- **courier_profiles**: user_id (PK/FK), address, wallet_number, wallet_type (`vodafone_cash|instapay`), vehicle_type, vehicle_color, plate_number, id_front_url, id_back_url (**encrypted**), vehicle_photo_url, approval_status (`pending|needs_fix|approved|rejected`), review_note, fix_fields, reviewed_by, reviewed_at, resubmitted_at, resubmission_count, debt, advance_active, is_online, rating_avg, rating_count
- **addresses**: id, user_id, label, lat, lng, street, building, floor, apartment, landmark, is_default
- **admins**: id, email (unique), password_hash, name, role (`admin|super_admin`), is_active, created_by, last_login_at, created_at, updated_at, super_admin_guard (generated, UNIQUE → max one super_admin)
- **otp_codes**: id, phone, code_hash, purpose (`signup|reset_password|courier_fix`), subject_type (`customer|courier|admin`), expires_at, attempts, request_ip, terms_version_id + user_agent (signup only), verified_at, verification_token_hash, verification_expires_at, consumed_at, created_at
- **auth_tokens**: id, token_hash (unique), principal_type (`user|admin`), principal_id, expires_at, revoked_at, created_at
- **categories**: id, name (unique among non-deleted, via generated `name_guard`), icon_key, icon_url (at least one), color (`#RRGGBB`), sort_order, is_active, created_by, created_at, updated_at, deleted_at
- **shops**: id, category_id, name, photo_url, street, lat, lng (DECIMAL 10,7), opens_at, closes_at (TIME, Africa/Cairo; closes < opens = past midnight, equal = 24h), is_open (manual switch), created_by, created_at, updated_at, deleted_at. API adds computed `openNow`
- **orders**: id, customer_id, courier_id (nullable), type, scope, description, pickup_lat/lng/label (optional), dropoff_* (**snapshot** of the address at order time), price_type (`fixed|customer`), delivery_fee, min_fee, max_fee, distance_km (DECIMAL 6,1, nullable), distance_source (`GOOGLE|ESTIMATED`, nullable), commission_rate (frozen at acceptance), commission_amount, payment_method (`cash|transfer`), status, cancelled_by, cancel_reason, accepted_at, picked_up_at, delivered_at, created_at
- **order_stops**: id, order_id, shop_id, seq, request_text, status (`pending|arrived|purchased`)
- **order_offers**: id, order_id, courier_id, price, status (`pending|accepted|rejected|withdrawn`)
- **order_status_history**: id, order_id, status, by_type (`customer|courier|admin|system`), by_id, created_at
- **invoices**: id, order_id, order_stop_id, total, photo_url, photo_lat, photo_lng, photo_taken_at, receipt_photo_url (optional)
- **invoice_items**: id, invoice_id, name, price, is_substitute
- **messages**: id, order_id, sender_id, type (`text|image|system`), body, image_url, created_at, read_at
- **ratings**: id, order_id (unique), customer_id, courier_id, stars, tags, comment
- **notifications**: id, user_id, type, title, body, link_type, link_id, read_at, created_at — texts from `messages.properties` (`notification.<type>.title/body`)
- **device_tokens**: user_id, token, platform
- **courier_ledger**: id, courier_id, type (`commission|payment|advance|advance_closed|points_redeem`), amount (signed), order_id, payment_id, created_at — **append-only**
- **commission_payments**: id, courier_id, amount, method, proof_url, status (`pending|confirmed|rejected`), reviewed_by, reviewed_at
- **points_transactions**: id, user_id, amount (signed), reason (`order|rating|gift|redeem`), order_id
- **points_redemptions**: id, user_id, points, value_egp, status (`available|used`), used_on_order_id
- **complaints**: id, order_id, from_user_id, against_user_id, title, body, status (`new|in_progress|closed`), resolution_note, handled_by
- **bans**: id, user_id, reason, starts_at, ends_at (null = permanent), created_by
- **application_config**: id, config_key (unique — `KEY` is a reserved word), value (text, `''` = not set yet), value_type (`INT|DECIMAL|STRING|BOOLEAN`), description, updated_by (admin), updated_at. Rows are seeded by Flyway; the API only updates existing keys
- **terms_versions**: id, audience (`CUSTOMER|COURIER`), version (int, unique per audience), content (TEXT, Arabic), published_at, is_current, created_by (admin), current_guard (generated, UNIQUE → one current per audience)
- **terms_acceptances**: id, user_id, terms_version_id (unique together), accepted_at, ip_address, user_agent — **append-only, triggers block UPDATE/DELETE**
- **audit_log**: id, admin_id, action (e.g. `courier.approval_changed`, `shop.created`, `config.updated`, `terms.published`), target_type, target_id, details (JSON text), created_at — append-only

## Application config (never hardcode these — read from `application_config`)
All business numbers live in the `application_config` table. Read them with `modules/setting/service/ApplicationConfigService` (`getInt`, `getDecimal`, `getString`, `getBoolean`, `findDecimal` for values that may be unset). Use `ConfigKeys` constants for the keys. The whole table is cached by Caffeine for `app.config.cache-ttl` (30 s) and evicted right after a change commits. New keys = a new Flyway migration that inserts the row.
Admin API: `GET /api/admin/config` (admin), `PUT /api/admin/config/{key}` `{value}` (**super_admin only**, validated by type, audited `config.updated`).

| key | type | default | meaning |
|---|---|---|---|
| base_fee | DECIMAL | 15 | shops order, first shop |
| extra_shop_fee | DECIMAL | 5 | shops order, each extra shop |
| max_shops_per_order | INT | 5 | |
| anything_min_fee | DECIMAL | 15 | anything order, min fee |
| anything_no_pickup_max | DECIMAL | 30 | anything order without pickup, max fee |
| anything_base_max | DECIMAL | 25 | anything with pickup, max fee up to the free km |
| anything_free_km | DECIMAL | 3.5 | road km included in the base max |
| anything_per_km_fee | DECIMAL | 7 | added to the max per **started** km beyond the free km |
| commission_rate | DECIMAL | 20 | % of the delivery fee, frozen on the order **at acceptance** |
| points_per_egp | DECIMAL | 1 | points on delivery = floor(delivery fee × this) |
| order_offer_ttl_minutes | INT | 10 | |
| order_search_ttl_minutes | INT | 30 | |
| service_area_lat / service_area_lng / service_area_radius_km | DECIMAL | **empty — the super admin must set them** | service-area circle (straight-line km). Until set, quotes return 503 `SERVICE_AREA_NOT_CONFIGURED` |

Not created yet — add them by migration when their phase is built: `debt_limit` (100), `advance_amount` (50), `points_per_rating` (2), `points_per_5_star` (5), `redeem_points` (1000), `redeem_value` (20 EGP).

## Order lifecycle (state machine)
```
new → accepted → to_shop → buying → picked_up → to_customer → delivered → closed
  \__________________________________________/
                     ↓
                 cancelled
```
Every transition must write a row to `order_status_history` and a `system` row to `messages` (these show inside the chat), and send a notification to the other party.

## Business rules — enforce on the server, never trust the client
### Ordering & pricing
All pricing lives in `modules/order/service/OrderPricingService`. The app previews it with `POST /api/v1/orders/quote`; order creation must recompute it on the server and never trust the client.
1. `type=shops`: price is **fixed** = `base_fee + extra_shop_fee × (shops − 1)`, with 1..`max_shops_per_order` shops. No offers. The first courier to accept gets it.
2. `type=anything`: the customer sets `delivery_fee`. Couriers either accept it or create an `order_offers` row with their own price. The customer accepts one offer → order becomes `accepted` with that price, and all other offers → `rejected`.
   **The customer fee and every courier offer must be inside the allowed range** (`requireInRange` → `FEE_OUT_OF_RANGE`):
   - No pickup point → `anything_min_fee .. anything_no_pickup_max` (15..30).
   - With a pickup point → `distanceKm` = **road** distance pickup → dropoff (Google Distance Matrix, driving, rounded to 0.1 km):
     - `distanceKm ≤ anything_free_km` → 15..25
     - otherwise → 15..`25 + 7 × ceil(distanceKm − 3.5)`
   - Store `distance_km`, `distance_source` (`GOOGLE|ESTIMATED`), `min_fee`, `max_fee` on the order, so the price can always be explained.
   - The dropoff must be inside the service area (`OUTSIDE_SERVICE_AREA`).
3. `commission_rate` = config `commission_rate` (%), **frozen on the order at acceptance**. `commission_amount = delivery_fee × rate / 100`. Changing the config never affects already-accepted orders.

### Road distance (Google Distance Matrix)
- `common/maps/DistanceService` → `GoogleDistanceMatrixClient` (`app.maps.provider=google`, Spring `RestClient`, 3 s timeout). The key comes from env `GOOGLE_MAPS_API_KEY`, stays on the server only, and is never logged.
- Call it only from the server (quote endpoint, order creation), never from the apps.
- Coordinates are rounded to 4 decimals. Google results are cached for 24 h (Caffeine, in memory).
- Google fails, there is no route, or there is no key → Haversine × `app.maps.fallback-road-factor` (1.3), a WARN log line, `distance_source = ESTIMATED`. Estimates are not cached.
- Tests use `support/FakeDistanceService` (`app.maps.provider=fake`): no real API calls. `GoogleDistanceMatrixClientTest` uses a mock HTTP server.

### Courier capacity
4. A courier can accept a new order **only if every active order they hold is `picked_up` or later.** Reject otherwise.
5. A courier with `users.status != active`, `approval_status != approved`, an active ban, or `is_online=false` cannot see or accept orders.

### Cancellation
6. **Customer** may cancel only while the order has **no invoices yet**.
7. **Courier** may cancel only **before `picked_up`**. On courier cancel: `courier_id = null`, status back to `new`, re-broadcast to couriers.
8. After `picked_up`, **nobody** can cancel (only an admin can intervene).

### Invoices
9. One invoice per `order_stop`. Photo must come from the in-app camera with GPS + timestamp (`photo_lat`, `photo_lng`, `photo_taken_at` are required). Gallery uploads are rejected.
10. Customer does **not** approve invoices — they are informational. First invoice locks customer cancellation (rule 6).

### Payment
11. Payment is between customer and courier only. `payment_method=transfer` means the customer sends money to `courier_profiles.wallet_number`. The backend does **not** verify customer payments. The courier confirms receipt at delivery.

### Commission, debt, advance
12. On `delivered`: insert `courier_ledger(type=commission, amount=−commission_amount)` and add to `courier_profiles.debt`.
13. If `debt ≥ debt_limit`: courier finishes their current orders, then `users.status = suspended` (no new orders).
14. Courier uploads a payment proof → insert `commission_payments(status=pending)` **and immediately** grant an advance: ledger `type=advance, +advance_amount`, `advance_active=true`, `status=active`.
15. Admin confirms → ledger `type=payment` for the amount, `type=advance_closed`, recompute `debt`, `advance_active=false`.
    Admin rejects → close the advance, `status=suspended`.
16. `courier_ledger` is append-only. `debt` must always equal the ledger sum; never edit `debt` directly without a ledger row.

### Points
17. On `delivered`: the customer and the courier **each** get `floor(delivery_fee × points_per_egp)` points (read `points_per_egp` from `application_config` at that moment), written to `points_transactions` (reason `order`). On rating: customer + `points_per_rating`; on a 5-star rating the courier gets + `points_per_5_star`.
18. Redeeming `redeem_points` creates a `points_redemptions` row worth `redeem_value`:
    - customer → discount applied automatically to their next order's final invoice
    - courier → ledger `type=points_redeem, +redeem_value` (reduces debt)

### Bans & complaints
19. Banned users (active row in `bans`) cannot place or deliver orders; return the ban reason to the client.
20. A customer who refuses to receive an order after `picked_up` can be banned by an admin.

## Security & privacy
- ID card images: encrypted at rest, private storage, short-lived signed URLs, viewable by admins only.
- OTP: hash codes, 5-minute expiry, rate-limit by phone and IP, max attempts.
- Admin passwords: bcrypt/argon2. Admin endpoints are separate from mobile endpoints.
- Customers see a courier's name, photo, rating, vehicle and wallet number **only** for their own active order. Couriers never see a customer's address before accepting (show distance/street only).
- Every admin action that touches money, bans, approvals or settings → `audit_log`.

## Conventions
- **Money (decided):** EGP with exactly 2 decimals (piasters). DB `DECIMAL(10,2)`; Java `BigDecimal` normalized with `Money.egp(...)` (never double/float); JSON a number with 2 decimals — `1.50` = 1 EGP 50 piasters, `15.00` = 15 EGP. Request money fields use `@EgpAmount` (≥ 0, ≤ 2 decimals, ≤ 99,999,999.99). Helpers: `common/util/Money` (`egp`, `fromPiasters`, `toPiasters`, `ZERO`).
- Timestamps in UTC, displayed in Africa/Cairo.
- API errors return a stable machine `code` plus a user-facing `message`. Messages must be in Egyptian Arabic because the apps display them as-is; keep them in a translations file, not inline in logic.
- Use transactions for anything that changes status + money together (delivery, payments, redemptions).
- Side effects (WhatsApp, deleting replaced files) go through `AfterCommit.run(...)` so a rollback never sends/deletes.
- Real-time (chat, order status, new orders feed): `[fill in: WebSockets / Firebase / Pusher]`.

## Don'ts
- Don't add a customer wallet or customer→app payments. That feature was removed on purpose.
- Don't add phone orders (admin creating orders for customers). Removed on purpose.
- Don't hardcode prices, commission, limits or points — use `application_config` via `ApplicationConfigService`.
- Don't delete orders, invoices, ledger rows or payments — use statuses.
- Categories and shops are **soft-deleted** (`deleted_at` + `@SQLRestriction`, invisible to all queries). A category with shops can't be deleted (`CATEGORY_HAS_SHOPS`).

## Open questions (ask before implementing)
- If a customer refuses delivery after `picked_up`, who bears the cost of the items?