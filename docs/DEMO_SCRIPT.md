# Demo script — showing this to an evaluator

A tight, rehearsable walkthrough that proves every requirement in the
problem statement, live, in about 10–12 minutes. Do a dry run once by
yourself before the real thing — the timings below assume you already know
where to click.

**Do this the night before**, not while your evaluator is watching:
finish sections 1–2 of the root `README.md` (installing everything,
creating the four databases) and confirm all five services start cleanly
once. That way the actual demo is just "run five commands, open two
windows."

---

## 0. Two minutes before they arrive

Open five terminals (or five run configurations in STS) and start, in
order, waiting ~15–20s between each: `eureka-server`, then
`auth-service` / `restaurant-service` / `payment-service` / `order-service`
(any order, one per terminal), then `api-gateway` last. Leave
[http://localhost:8761](http://localhost:8761) open in a browser tab.

Also have open: Postman with the collection imported, and a terminal in
the `frontend` folder with `npm run dev` already running.

---

## 1. "This is a real microservices system" (Eureka + STS) — 2 min

- Point at the **Eureka dashboard** (:8761): "Every one of these five boxes
  is a separate, independently-deployable Spring Boot application —
  Auth, Restaurant, Order, Payment, and the Gateway — each registered
  itself here on startup. This is service discovery."
- Switch to **STS / your IDE**: show the Project Explorer with all six
  Maven projects (`eureka-server`, `api-gateway`, `auth-service`,
  `restaurant-service`, `order-service`, `payment-service`) each as its own
  independent project with its own `pom.xml` — "no shared codebase, no
  shared database; each service owns its own schema."
- Briefly open `order-service`'s `OrderService.java` and point at the
  `RestaurantClient` / `PaymentClient` fields: "This is how Order talks to
  Restaurant and Payment — not by hard-coded URLs, but by service name,
  resolved through Eureka."

## 2. "It's authenticated end-to-end with JWT" (Postman) — 3 min

Run the Postman collection folders **in order**, narrating each:

1. **1. Auth** → *Register customer* and *Register restaurant owner*.
   Point at the response: "Every register/login returns a signed JWT —
   this is what every other request will carry."
2. **2. Restaurant setup (owner)** → *Create restaurant*, *Add menu item*.
   "This request is carrying the owner's JWT in the Authorization header —
   try removing it and re-sending, you'll get a 401." (Actually remove the
   header once, live, to show the 401 — it's a good moment.)
3. Show *List restaurants (public)* — no token needed: "browsing is public,
   ordering isn't."

## 3. "The order flow really talks across three services" — 3 min

Continue the collection:

1. **3. Ordering (customer)** → *Place order*. Open the response and walk
   through it: `status: PAYMENT_COMPLETED` (or `PAYMENT_FAILED` — the
   simulated gateway declines about 8% of charges on purpose, so if you
   land on a decline, say so: "that's the simulated payment gateway
   randomly declining this one, exactly like a real card would — the order
   safely stopped at `PAYMENT_FAILED` instead of proceeding". Just place
   the order again for a clean run).
2. **4. Fulfilment (owner)** → *List incoming orders*, then *Accept order*,
   then *Advance to PREPARING → OUT_FOR_DELIVERY → DELIVERED*. Each call
   is the restaurant owner moving the same order through its lifecycle.
3. **5. After delivery (customer)** → *Rate the order*, then *Confirm
   restaurant rating updated* — show the restaurant's `rating` /
   `ratingCount` fields changed. "That rating call went Order → Restaurant
   again — a second, independent inter-service path."

## 4. "It's resilient, not just functional" — 2 min

This is the section that separates a working demo from an *impressive*
one — most student projects don't show this.

- **Circuit breaker:** stop the `payment-service` terminal (Ctrl+C).
  Re-run *Place order* a few times in Postman — after a handful of failed
  calls you'll see them start failing instantly (not timing out) with a
  clear "Payment service is temporarily unavailable" message, because the
  circuit has opened. Show `http://localhost:8083/actuator/health` —
  the `paymentService` circuit breaker's state is visible right there.
  Restart `payment-service`, wait ~10s, and place an order again — it
  succeeds once the breaker recovers.
- **Rate limiting:** in Postman, right-click any simple `GET` request
  (e.g. *List restaurants*) → **Run** → set iterations to 100 → Run. After
  the configured limit (60 requests / 10s by default) you'll start seeing
  `429 Too Many Requests` responses — "this is what protects the platform
  during a lunch-hour traffic spike: one misbehaving client can't starve
  everyone else."

## 5. "And it's a real, usable product" (the frontend) — 2 min

Switch to the browser at `localhost:5173`:

- Show the customer side: browse restaurants, open one, add a couple of
  items, check out.
- Switch to (or open an incognito window as) the restaurant owner: show
  the **stats strip** (today's orders/revenue), the incoming-orders board,
  and accept the order you just placed — it's the same order you can now
  see moving through statuses on the customer's "My orders" page too.

## 5a. The four "extra" features — 3 min (do this if you have time)

These go beyond the base requirements and are worth calling out by name:

- **Photo uploads.** On the owner dashboard, click the restaurant's cover
  photo tile (or a menu item's thumbnail) and upload a picture — it shows
  up immediately on the customer-facing restaurant page.
- **Demand-driven surge pricing.** Place an order during a lunch (12–3pm)
  or dinner (7–10:30pm) window — the confirmed order shows a "⚡ demand
  surcharge" line with the base price and the reason. (Outside those
  windows, place 5 orders back-to-back at the same restaurant without
  accepting them — the 5th+ will trigger the "high demand" surcharge
  instead, so you can demo this any time of day.)
- **Live delivery tracking.** As the owner, walk an order to
  `OUT_FOR_DELIVERY`. Switch to the customer's "My orders" page — a small
  animated map appears showing a rider moving from the restaurant toward
  the delivery address, with a live ETA countdown and an assigned delivery
  partner's name/phone. Say plainly that this is simulated (no real GPS),
  which is honest and still genuinely impressive — the position is
  computed live from elapsed time, not a canned animation.
- **Order/delivery emails.** By default these just log to the
  `order-service` console (open that terminal window and place an order —
  point at the `[EMAIL DISABLED - would send]` log line). If you set up a
  Gmail app password beforehand (see README section 7a), enable it and
  place a real order to show an actual email arriving.

## 6. Closing line

"Every piece of the problem statement is here and actually working, not
just described: JWT auth, an API Gateway, Eureka registration and load
balancing, three services that genuinely call each other over the network,
a circuit breaker and rate limiting for the peak-hour resilience
requirement, automated tests per service, and a working front end on top."

---

## Requirement checklist (for your own reference / report)

| Requirement | Where it lives |
|---|---|
| JWT authentication | `auth-service` issues; Gateway + every service validates |
| Restaurant Service manages menus | `restaurant-service` |
| Order Service handles orders | `order-service` (state machine in `OrderService`) |
| Payment Service processes transactions | `payment-service` (`PaymentGatewaySimulator`) |
| Inter-service communication | `order-service` → `restaurant-service` (menu validation, rating) and → `payment-service` (charge, refund), via Feign |
| Register with Eureka | every service's `application.yml` + `@EnableDiscoveryClient` |
| API Gateway | `api-gateway`, Spring Cloud Gateway routes |
| Load balancing | Eureka + Spring Cloud LoadBalancer (used automatically by Feign and the Gateway's `lb://` routes) |
| High-traffic handling | Resilience4j circuit breaker (Order → Payment) + Gateway rate limiting |
| Testing | JUnit 5 + Mockito unit tests per service (`mvn test`) |
| Deployment | Each service is an independently runnable Spring Boot jar (`mvn clean package` → `java -jar target/*.jar`) |
| Extra: surge pricing, delivery tracking, photo uploads, email | `SurgePricingService`, `DeliverySimulator`/`getTracking`, `FileStorageService`, `NotificationService` — all in the "extra" folder of talking points, see section 5a |
