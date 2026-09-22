# Architecture

## Why these service boundaries

Each service owns exactly one business capability and one database — no
service reaches into another service's tables. This is the standard
"database per service" pattern: it's what makes each service independently
deployable (you can redeploy `restaurant-service` without touching
`order-service`) and independently scalable (you could run three instances
of `order-service` during a lunch rush and one instance of `auth-service`,
since login traffic is comparatively steady).

| Service | Owns | Does NOT do |
|---|---|---|
| **auth-service** | Users, passwords, JWT issuance | Never sees menus, orders, or payments |
| **restaurant-service** | Restaurant profiles, menus, availability | Never touches order or payment data |
| **order-service** | Order lifecycle, orchestration | Never processes payment itself — delegates to payment-service |
| **payment-service** | Payment records, simulated gateway | Never knows what was ordered, only an amount + order id |
| **api-gateway** | Routing, JWT enforcement at the edge, CORS | Holds no business data of its own |
| **eureka-server** | Service registry | Holds no business data of its own |

## Request flow: placing an order

1. Browser → **API Gateway** (`POST /api/orders`) with the customer's JWT.
2. Gateway validates the JWT, forwards the request to a healthy
   `order-service` instance (chosen via Eureka + Spring Cloud LoadBalancer),
   and adds `X-User-*` context headers.
3. `order-service` calls `restaurant-service` (via a Feign client, again
   resolved through Eureka) to fetch the restaurant and confirm it's open,
   and to fetch each menu item's *current* price and availability — this
   guards against a stale price the client might have cached.
4. `order-service` persists the order with status `CREATED`.
5. `order-service` calls `payment-service` (via `PaymentGatewayClient`,
   wrapped in a Resilience4j circuit breaker) to charge the order.
6. Based on the result, the order moves to `PAYMENT_COMPLETED` or
   `PAYMENT_FAILED` and is returned to the customer.
7. If paid, the order becomes visible to the restaurant owner's "incoming
   orders" view, who can accept or reject it.

## Resilience: the payment circuit breaker

`order-service` never calls `payment-service` directly — it goes through
`PaymentGatewayClient`, a small wrapper annotated with Resilience4j's
`@CircuitBreaker`. Configuration lives in `order-service/application.yml`
under `resilience4j.circuitbreaker.instances.paymentService`:

- Tracks the last 10 calls (`sliding-window-size: 10`).
- If 50%+ of them fail (`failure-rate-threshold: 50`), the circuit opens.
- While open, calls fail immediately via the fallback method (which marks
  the order `PAYMENT_FAILED` with a clear message) instead of waiting on a
  dependency that's already struggling — exactly the protection you want
  during a traffic spike where `payment-service` is under load.
- After 10 seconds (`wait-duration-in-open-state`), it allows a few test
  calls through (`half-open` state) to see if `payment-service` has
  recovered.

The Feign clients also carry a bounded retry (`FeignRetryConfig`) for
short, transient blips — three attempts with a short backoff — separate
from the circuit breaker, which handles sustained outages.

## Authentication model

`auth-service` is the only service that ever sees a password. It issues a
JWT containing `userId`, `role` and `fullName` claims, signed with a shared
HMAC secret (`JWT_SECRET`, identical across every service).

Two layers check that token:
1. **API Gateway** — a `GlobalFilter` rejects any request to a protected
   route that's missing a valid token, before it's ever routed anywhere.
2. **Each resource service** (`restaurant-service`, `order-service`,
   `payment-service`) — its own `JwtAuthFilter` independently validates the
   same token and populates Spring Security's context with the user's id
   and role. This means a service is still secure even if it were ever
   reachable directly (bypassing the gateway) — defense in depth rather
   than trusting the network path alone.

Authorization (as opposed to authentication) is then just normal
application logic: e.g. `RestaurantService.updateRestaurant` checks that
the restaurant's `ownerId` matches the caller's id before allowing an edit.

## Data snapshotting

`OrderItem` stores a *snapshot* of each item's name and price at the moment
the order was placed (`itemName`, `unitPrice`), rather than only a foreign
key into `restaurant-service`. If a restaurant later changes a dish's price
or renames it, historical orders still show exactly what the customer
actually paid for.

## What would change to run this at real "lunch rush" scale

- Run 2–3 instances each of `order-service` and `payment-service` (the two
  busiest paths); Eureka + the load balancer handle the rest automatically.
- Move `resilience4j` thresholds from local-friendly defaults to values
  tuned against real traffic/error-rate data.
- Add a message broker (Kafka/RabbitMQ) for the accept/reject and status
  notifications instead of the frontend's 15-second polling, so restaurant
  dashboards update instantly under load.
- Add Flyway migrations and switch `ddl-auto` to `validate`.
- Put a real payment processor behind `PaymentGatewaySimulator`'s interface.

## Surge pricing (`order-service`)

`SurgePricingService` is a small, pure component: given a base amount and
how many orders are already weighing on a restaurant's kitchen, it returns
a multiplier and a human-readable reason. It takes a `java.time.Clock`
(injected via `ClockConfig`) rather than calling `LocalTime.now()` directly
- this is what let the unit tests pin "the current time" to a known
off-peak moment instead of becoming flaky depending on when the test suite
happened to run. Two independent triggers can stack: a peak dining window
(12–3pm or 7–10:30pm) and a restaurant already having 5+ orders in
`PAYMENT_COMPLETED`/`ACCEPTED`/`PREPARING` right now.

## Simulated delivery tracking (`order-service`)

`DeliverySimulator` "assigns" a delivery partner (name, phone, ETA, and a
destination point jittered near the restaurant) the moment an order moves
to `OUT_FOR_DELIVERY`. `OrderService.getTracking` then computes the rider's
current position as a pure function of elapsed time versus that ETA - it
never needs to be pushed or ticked by a background job, which is why the
frontend can just poll `GET /orders/{id}/tracking` every few seconds and
always get a consistent answer. The destination is deliberately *not* real
geocoding of the customer's typed address (that needs a paid mapping API);
it's a stand-in close to the restaurant so the map has two believable,
separated points to draw a route between.

## Photo uploads (`restaurant-service`)

`FileStorageService` writes uploaded restaurant/menu photos to a local
`uploads/` folder (configurable via `app.upload-dir`), validates file type
and a 5MB size cap, and generates a random filename so uploads can never
collide or be used to overwrite an arbitrary path. `StaticResourceConfig`
serves that folder back out at `GET /images/**`, which the Gateway also
exposes at `/api/images/**` so the frontend only ever talks to one host.
This is intentionally the simplest thing that works for a single-instance
local deployment - see the README's deployment notes for what to swap in
before running more than one `restaurant-service` instance.

## Notifications (`order-service`)

`NotificationService` sends order-confirmation and delivery-confirmation
emails. Every path is wrapped so a missing customer record, an unreachable
`auth-service`, or a misconfigured/unreachable SMTP server only ever logs a
warning - it can never fail the order itself. It also runs `@Async`, off
the request thread, so even a slow mail server can't add latency to
checkout. `notifications.email.enabled` defaults to `false`, in which case
it logs what it *would* have sent instead of opening any network
connection at all.
