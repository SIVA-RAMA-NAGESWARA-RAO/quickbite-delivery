# QuickBite Delivery

A demand-driven food ordering platform built as a set of independent Spring
Boot microservices behind a single API Gateway, with a React front end for
customers and restaurant owners.

```
Restaurant Service   — restaurant profiles, menus, photo uploads
Order Service        — order lifecycle, surge pricing, delivery tracking, emails
Payment Service       — simulated payment processing
Auth Service          — registration, login, JWT issuance
API Gateway           — single entry point, JWT enforcement, routing, rate limiting
Eureka Server         — service registry / discovery / load balancing
```

Beyond the core ordering flow, this includes: **demand-based surge pricing**
(peak-hour and high-demand surcharges), **simulated live delivery tracking**
(a moving rider on a map with ETA), **restaurant/menu photo uploads**,
**order confirmation & delivery emails**, and **gateway rate limiting** — see
section 7 for how each works.

Everything here runs **directly on your machine** — no Docker required.
You need a JDK, Maven, PostgreSQL and Node.js installed locally. If you've
never done any of this before, follow the steps below in order and you'll
have the whole platform running in well under an hour.

**Want a live, shareable link instead of (or in addition to) running it
locally?** See [`docs/DEPLOY.md`](docs/DEPLOY.md) — Vercel for the frontend,
Railway for the backend, with honest cost expectations. That's a separate,
optional step; everything below is about running it on your own machine.

---

## 0. Windows fast path (recommended if that's what you're on)

If you're on Windows, five `.bat` files at the project root do most of this
for you:

1. Double-click **`check-prerequisites.bat`** — tells you exactly what's
   missing (Java, Maven, Node, PostgreSQL) before you go any further.
2. Double-click **`setup-database.bat`** — creates the `quickbite` role and
   the four databases. It will ask for your **PostgreSQL superuser
   password** (the one you set when installing PostgreSQL — for example,
   type `root` if that's what you chose). This is safe to run more than once.
3. Double-click **`start-all.bat`** — opens all six backend services, each
   in its own window, in the correct order. Give it 1–2 minutes, then check
   [http://localhost:8761](http://localhost:8761) to confirm all five
   services (Auth, Restaurant, Order, Payment, Gateway) show up registered.
4. Double-click **`start-frontend.bat`** — installs frontend dependencies
   (first time only) and starts the website.
5. **`stop-all.bat`** closes everything when you're done.

If something fails, sections 1–2 and 9 below explain what each error means
and how to fix it — `check-prerequisites.bat` and `setup-database.bat`'s own
output will usually already tell you exactly what's wrong.

On macOS/Linux, or if you'd rather understand and run each step by hand
(recommended before an evaluation, so you can explain what's happening),
continue with section 1 below.

---

## 1. What you need installed

| Tool | Version | Check with |
|---|---|---|
| Java (JDK) | 17 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| PostgreSQL | 14+ | `psql --version` |
| Node.js | 18+ | `node -v` |
| An IDE | VS Code or Spring Tool Suite (STS) | — |

If any of these are missing:

- **Java 17**: [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17) (pick your OS, run the installer).
- **Maven**: [maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) — unzip and add its `bin` folder to your PATH. On Windows, search "Environment Variables" in the Start menu to add it; on macOS/Linux, `brew install maven` or your package manager is easiest.
- **PostgreSQL**: [postgresql.org/download](https://www.postgresql.org/download/) — during setup it will ask you to set a password for the `postgres` superuser; remember it, you'll need it once below.
- **Node.js**: [nodejs.org](https://nodejs.org/) — install the LTS version.
- **VS Code**: [code.visualstudio.com](https://code.visualstudio.com/), then install the **"Extension Pack for Java"** and **"Spring Boot Extension Pack"** from the Extensions panel. Or use **Spring Tool Suite (STS)** from [spring.io/tools](https://spring.io/tools), which comes with everything pre-installed.

---

## 2. Create the databases

Each service owns its own database (a standard microservices practice — no
service ever reads another service's tables directly).

**Easiest way:** run `db/init.sql` against your PostgreSQL server. It
creates a `quickbite` login and all four databases, and is safe to run more
than once (it skips anything that already exists instead of erroring):

```bash
psql -U postgres -f db/init.sql
```

You'll be prompted for your **PostgreSQL superuser password** — the one you
set when you installed PostgreSQL (not the `quickbite` app password used
below). On Windows, `setup-database.bat` runs this same script for you.

**By hand instead**, if you'd rather see each step: log into PostgreSQL
(`psql -U postgres`) and run:

```sql
CREATE USER quickbite WITH PASSWORD 'quickbite';
CREATE DATABASE authdb       OWNER quickbite;
CREATE DATABASE restaurantdb OWNER quickbite;
CREATE DATABASE orderdb      OWNER quickbite;
CREATE DATABASE paymentdb    OWNER quickbite;
\q
```

That's it — you don't need to create any tables by hand. Every service uses
Hibernate's `ddl-auto: update`, so tables are created automatically the
first time each service starts.

> Using pgAdmin instead of the command line? Just create a login role named
> `quickbite` with password `quickbite`, then create the four databases
> above with that role as owner.

If you'd rather not create a dedicated role, you can instead point every
service at your existing `postgres` user by setting three environment
variables before you start each one (see section 4): `DB_USER=postgres`,
`DB_PASSWORD=<your password>`, and leave `DB_HOST`/`DB_PORT` at their
defaults. Either way, **the app services never need your postgres superuser
password** — only this one-time setup step does.

---

## 3. Open the project

Each folder in this repository (`eureka-server`, `api-gateway`,
`auth-service`, `restaurant-service`, `order-service`, `payment-service`) is
its own independent Maven project — that's intentional; it's what lets you
build, version and deploy each microservice separately in a real
organization.

**In VS Code:** open the root `quickbite-delivery` folder
(`File → Open Folder…`). The Java extension will detect all six `pom.xml`
files automatically and show them in the "Java Projects" panel.

**In Spring Tool Suite:** `File → Import → Maven → Existing Maven
Projects`, browse to the root `quickbite-delivery` folder, and tick all six
projects when they're listed.

You don't need to run anything from the IDE yet — the next section covers
starting everything, either from a terminal or from inside your IDE,
whichever you prefer.

---

## 4. Run the services — in this order

Each service is a normal Spring Boot app. **Open a separate terminal window
per service** (you'll have 5 terminals open for the backend) and run:

```bash
cd <service-folder>
mvn spring-boot:run
```

Or, in VS Code / STS, just open the `*Application.java` file for that
service and click **Run**.

### Startup order matters — follow this sequence:

**Step 1 — Eureka Server first, and wait for it.**
```bash
cd eureka-server
mvn spring-boot:run
```
Wait until you see `Started EurekaServerApplication` in the logs, then open
[http://localhost:8761](http://localhost:8761) in a browser — you should
see the Eureka dashboard (empty for now, that's expected).

**Step 2 — the four backend services, in any order (each in its own terminal):**
```bash
cd auth-service        && mvn spring-boot:run
cd restaurant-service   && mvn spring-boot:run
cd payment-service      && mvn spring-boot:run
cd order-service        && mvn spring-boot:run
```
Give each one 15–30 seconds to fully start and register itself. Refresh
[http://localhost:8761](http://localhost:8761) — you should see
`AUTH-SERVICE`, `RESTAURANT-SERVICE`, `PAYMENT-SERVICE` and `ORDER-SERVICE`
appear under "Instances currently registered with Eureka".

**Step 3 — the API Gateway, last (it needs the others in the registry to route to them):**
```bash
cd api-gateway
mvn spring-boot:run
```
Once it's up, `API-GATEWAY` will also appear in the Eureka dashboard, and
`http://localhost:8080` is now the single address the front end (and
Postman) will talk to.

### Ports reference

| Service | Port | Direct Swagger UI |
|---|---|---|
| Eureka Server (dashboard) | 8761 | — |
| API Gateway (**use this one**) | 8080 | — |
| Auth Service | 8081 | http://localhost:8081/swagger-ui.html |
| Restaurant Service | 8082 | http://localhost:8082/swagger-ui.html |
| Order Service | 8083 | http://localhost:8083/swagger-ui.html |
| Payment Service | 8084 | http://localhost:8084/swagger-ui.html |

Everyday use goes through the gateway at **`http://localhost:8080/api/...`**;
the individual Swagger pages are there so you can inspect or try out one
service's API in isolation while developing.

---

## 5. Run the front end

In a sixth terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the URL it prints (typically [http://localhost:5173](http://localhost:5173)).
The app is already configured (via `.env`) to talk to the gateway at
`http://localhost:8080/api`. If you ever run the gateway on a different
port, edit `frontend/.env`.

You now have a working app: register as **"I'm hungry"** to browse and
order, or **"I run a restaurant"** to set up a restaurant and manage
incoming orders.

---

## 6. Testing everything end-to-end

**Showing this to someone?** See
[`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md) for a rehearsed, ~10-minute
walkthrough (Eureka, STS, Postman, the circuit breaker, rate limiting, and
the front end) that maps directly back to the PS014 requirement list.


**Option A — the UI.** Register a restaurant-owner account, create a
restaurant and add a couple of menu items, then register a second account
(a different browser or an incognito window) as a customer, place an
order, and watch it show up on the owner's "Incoming orders" board.

**Option B — Postman.** Import `postman/QuickBite-Delivery.postman_collection.json`
into Postman. It's set up to run top-to-bottom against a fresh database:
it registers a customer and an owner, creates a restaurant and menu item,
places an order, and walks it through accept/reject and status updates,
storing IDs and tokens as it goes so you don't have to copy anything by hand.

**Option C — automated unit tests.** Each backend service has its own test
suite (business logic — registration/login rules, ownership checks, the
order state machine, payment idempotency — using JUnit 5 and Mockito).
Run them per service:
```bash
cd auth-service && mvn test
cd restaurant-service && mvn test
cd order-service && mvn test
cd payment-service && mvn test
```

---

## 7. How it fits together

```
                        ┌─────────────────┐
                        │  Eureka Server   │  (service registry, :8761)
                        └────────▲─────────┘
                    registers/discovers │
        ┌──────────────┬──────────┼──────────┬───────────────┐
        │              │          │          │               │
┌───────┴─────┐ ┌──────┴──────┐ ┌─┴────────┐ ┌┴────────────┐  │
│ Auth Service│ │ Restaurant  │ │  Order   │ │  Payment    │  │
│   :8081     │ │ Service     │ │ Service  │ │  Service    │  │
│             │ │  :8082      │ │  :8083   │ │   :8084     │  │
└─────────────┘ └─────────────┘ └────┬─────┘ └──────▲──────┘  │
                                       │ Feign calls  │         │
                                       └──────────────┘         │
                                (Order → Restaurant, Order → Payment)
                                                                 │
                        ┌────────────────────────────────────────┘
                        │
                ┌───────┴────────┐        ┌──────────────┐
                │  API Gateway   │◄───────►│ React front  │
                │    :8080       │  HTTPS  │ end  :5173   │
                └────────────────┘         └──────────────┘
```

- **JWT authentication.** `auth-service` issues signed JWTs on
  register/login. The **API Gateway** validates every protected request's
  token before it's allowed through (public exceptions: login, register,
  and browsing restaurants/menus). Each backend service *also* validates
  the token independently — so the platform stays secure even if a service
  is ever called directly instead of through the gateway.
- **Eureka + load balancing.** Every service registers itself with Eureka
  under a logical name (e.g. `order-service`). The Gateway and Order
  Service's Feign clients look services up by that name, and Spring Cloud
  LoadBalancer picks a healthy instance — so if you ever ran two instances
  of `restaurant-service` to handle a lunchtime surge, traffic would
  automatically spread across both with zero configuration changes.
- **Order → Restaurant, Order → Payment.** Placing an order validates the
  restaurant and menu prices live against `restaurant-service`, then calls
  `payment-service` to charge the order. A **circuit breaker** (Resilience4j)
  wraps the payment call: if `payment-service` is struggling or down, the
  breaker trips and fails fast with a clear error instead of letting
  requests pile up.
- **Order state machine.**
  `CREATED → PAYMENT_COMPLETED/PAYMENT_FAILED → ACCEPTED/REJECTED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`
  (or `CANCELLED` by the customer before acceptance). A rejected or
  cancelled *paid* order automatically triggers a refund call to
  `payment-service`. Once `DELIVERED`, the customer can rate the order once;
  that rating is forwarded to `restaurant-service` (a second, independent
  Order → Restaurant call) which recomputes the restaurant's overall rating.
- **Rate limiting.** The API Gateway caps how many requests any one client
  (by user, once authenticated, or by IP before that) can make per time
  window, returning `429 Too Many Requests` once exceeded — the platform's
  first line of defense during a genuine traffic spike, before any request
  even reaches a backend service. Configurable via `RATE_LIMIT_REQUESTS`
  and `RATE_LIMIT_WINDOW_SECONDS`.
- **Demand-driven surge pricing.** `order-service` checks two things when
  an order is placed: is it a known peak dining window (12–3pm, 7–10:30pm),
  and does the target restaurant already have a backlog of unfulfilled
  orders? Either one applies a transparent surcharge — the customer always
  sees the base price, the multiplier, and *why*, never a silent markup.
- **Simulated live delivery tracking.** Once a restaurant marks an order
  `OUT_FOR_DELIVERY`, a delivery partner (name, phone, ETA) is assigned and
  `GET /orders/{id}/tracking` interpolates their position over time. This
  is honestly simulated — there's no real GPS or geocoding involved, which
  would need a paid mapping API — but the math (elapsed time vs. ETA,
  position interpolation) is real, and the frontend polls it every few
  seconds to animate a moving rider on a simple built-in map.
- **Photo uploads.** Restaurant owners can upload a cover photo and
  per-dish photos directly from the dashboard. Files are stored on local
  disk (`app.upload-dir`, default `./uploads`) and served back out through
  the gateway at `/api/images/...` — no cloud storage account needed for a
  local/demo deployment.
- **Order & delivery emails.** `order-service` sends a confirmation email
  when payment succeeds and another when the order is marked `DELIVERED`.
  This is **off by default** (`notifications.email.enabled: false`) so a
  fresh checkout works with zero mail setup — it just logs what it *would*
  have sent. See section 7a below to turn on real emails.

### 7a. Turning on real order emails (optional)

By default, order/delivery "emails" are only logged to the `order-service`
console — nothing is sent, and nothing can fail because of it. To send
real emails (handy for a live demo), the easiest option is a free Gmail
app password:

1. Turn on 2-Step Verification on the Gmail account you want to send from,
   then create an **App Password** at
   [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
2. Before starting `order-service`, set these environment variables (or
   edit `order-service/src/main/resources/application.yml` directly):
   ```
   NOTIFICATIONS_EMAIL_ENABLED=true
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=youraddress@gmail.com
   MAIL_PASSWORD=<the 16-character app password, not your normal Gmail password>
   ```
3. Restart `order-service`. Placing an order (and later marking it
   `DELIVERED`) will now email the customer's real registered address.

If email sending ever fails (wrong password, no internet, etc.),
`order-service` logs a warning and the order still goes through completely
normally — notifications are always best-effort, never a hard dependency.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for more detail on each
service's responsibilities and design decisions.

---

## 8. Project layout

```
quickbite-delivery/
├── *.bat                 Windows one-click setup/start/stop scripts (see section 0)
├── db/init.sql           Idempotent database + role creation script
├── docker-compose.yml    Full-stack Docker deployment (VPS / local sanity-check)
├── .env.example          Config template for docker-compose
├── eureka-server/        Service registry (+ Dockerfile for deployment)
├── api-gateway/          Routing + JWT enforcement + CORS + rate limiting (+ Dockerfile)
├── auth-service/         Registration, login, JWT issuing (+ Dockerfile)
├── restaurant-service/   Restaurant + menu management, photo uploads (+ Dockerfile)
├── order-service/        Order lifecycle, surge pricing, delivery tracking, emails (+ Dockerfile)
├── payment-service/      Simulated payment processing (+ Dockerfile)
├── frontend/             React (Vite) app (+ vercel.json for one-click Vercel deploy)
├── postman/              Ready-to-run Postman collection
└── docs/                 Architecture notes, demo script, deployment guide, API testing guide
```

Each backend folder follows the same internal layout:
`controller/ → service/ → repository/ → entity/`, plus `dto/`, `security/`,
`exception/` and `config/` as needed. This is a standard layered
architecture, chosen so anyone opening any one service for the first time
already knows where to look.

---

## 9. Troubleshooting

- **A service crashes on startup with a long stack trace ending in
  something like `ConnectionFactory.openConnection` / `PSQLException` /
  "the database system is starting up" / "database ... does not exist".**
  This is the most common first-run error, and it almost always means the
  databases from section 2 were never created (or PostgreSQL isn't running
  at all). Fix: run `setup-database.bat` (or `psql -U postgres -f db/init.sql`
  by hand), and confirm PostgreSQL is actually running first — see the next
  point. This is **not** a bug in the code; every one of the four services
  will fail the exact same way until its database exists.
- **A service won't start / "port already in use".** Something else on
  your machine is using that port. Either stop it, or override the port
  for that one run: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9091`.
- **"Connection refused" to Postgres.** Make sure the PostgreSQL service is
  actually running (on Windows, check the "Services" app for "postgresql-x64-...";
  on macOS, `brew services list`; on Linux, `sudo systemctl status postgresql`).
  `check-prerequisites.bat` runs a quick `pg_isready` check for you.
- **`setup-database.bat` / `psql` asks for a password and nothing seems to
  work.** That password is your **PostgreSQL superuser** password (set
  during installation) — not anything related to the app itself. If you
  don't remember it, it's whatever you typed into the PostgreSQL installer's
  "password" screen; on some installations it defaults to `postgres`.
- **A service starts but the Gateway returns 503 for it.** Give it another
  30 seconds — Eureka instances take a short while to propagate — then
  check the Eureka dashboard at :8761 to confirm it's actually registered.
- **Frontend shows "Could not reach the server".** Confirm the gateway is
  running on :8080 and `frontend/.env` points at it.
- **Changed the JWT secret and now everything returns 401.** All five
  services must share the exact same `JWT_SECRET` value (they default to
  the same placeholder value out of the box — only change it if you also
  update it everywhere, e.g. via a `JWT_SECRET` environment variable set
  identically before starting each service).
- **Uploaded a photo but it doesn't show up.** Confirm `restaurant-service`
  is running and check its console for errors; uploaded files are written
  under `restaurant-service/uploads/` by default — if that folder can't be
  created (e.g. a permissions issue), the upload endpoint will return a
  clear error rather than failing silently.
- **Getting "429 Too Many Requests".** That's the gateway's rate limiter —
  see section 7 — working as intended. Wait a few seconds, or raise
  `RATE_LIMIT_REQUESTS` / `RATE_LIMIT_WINDOW_SECONDS` if it's tripping
  during normal use.

---

## 10. Before you actually deploy this anywhere

This project is built to be functionally production-shaped (real JWT auth,
real service discovery, real load balancing, a real circuit breaker,
proper layered services, real automated tests) but a few things are
intentionally simplified for a local/demo environment and should be
revisited before a real deployment:

- **Payment gateway is simulated.** `payment-service` approves/declines
  transactions itself (see `PaymentGatewaySimulator`) rather than calling a
  real processor like Razorpay/Stripe. Swap that one class out for a real
  client when you're ready to take real payments.
- **JWT secret.** The default secret in every `application.yml` is a
  placeholder. Set a strong, random `JWT_SECRET` environment variable
  (identically across all five services) before deploying anywhere real.
- **`ddl-auto: update`** is convenient for local development but isn't
  recommended for a production database — switch to a migration tool
  (Flyway/Liquibase) with `ddl-auto: validate` when you're ready.
- **Single Postgres instance.** Locally, one Postgres server hosts all four
  databases for convenience. In a real deployment you'd typically give each
  service its own database instance/cluster for true isolation.
- **Delivery tracking is simulated.** There's no real GPS, no real delivery
  fleet, and the "destination" is approximated near the restaurant rather
  than geocoded from the actual delivery address (real geocoding needs a
  paid mapping API). The position math (interpolating by elapsed time) is
  real; the coordinates it operates on are not.
- **Uploaded photos live on local disk.** Fine for one machine; move
  `FileStorageService` to a real object store (S3, Cloudinary, etc.) before
  running more than one instance of `restaurant-service`, since local disk
  storage isn't shared between instances.
- **Email is off by default.** Turning it on (section 7a) sends real SMTP
  email straight from `order-service`; for real production traffic you'd
  typically route this through a dedicated transactional email service
  (SendGrid, SES, etc.) instead, and move sending to a queue rather than
  an in-process `@Async` call.

---

## 11. Deploying this somewhere live

Everything above is about running the platform on your own machine. If you
want a real URL you can send to someone else, see
[`docs/DEPLOY.md`](docs/DEPLOY.md): Vercel for the frontend (free), and a
comparison of backend hosts with real, current pricing (Railway, Render, or
a plain VPS with the `docker-compose.yml` in this repo) so you can pick
based on what you actually need rather than a marketing page. I can't run
that deployment for you — it needs your own hosting accounts — but every
config file it references (`Dockerfile` per service, `docker-compose.yml`,
`frontend/vercel.json`) is already in this repo, ready to use.
