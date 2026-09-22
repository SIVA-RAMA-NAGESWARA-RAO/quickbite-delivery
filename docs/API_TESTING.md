# API testing guide

## Quickest path: Postman

1. Import `postman/QuickBite-Delivery.postman_collection.json` into Postman.
2. Make sure all five backend services + the gateway are running (see the
   root README, section 4).
3. Run the folders top to bottom: **1. Auth → 2. Restaurant setup (owner)
   → 3. Ordering (customer) → 4. Fulfilment (owner) → 5. After delivery
   (customer)**. Each request's response feeds the next one automatically
   via collection variables (`customerToken`, `ownerToken`, `restaurantId`,
   `menuItemId`, `orderId`) — you don't need to copy anything by hand.
4. Re-run from step 1 any time you want a clean run (it registers new
   accounts each time — change the email addresses in the two "Register"
   requests if you get a "already exists" error from re-running against
   the same database).

## Exploring one service in isolation

Every backend service ships its own Swagger UI, useful when you're working
on that one service and don't want to go through the full flow:

- Auth: http://localhost:8081/swagger-ui.html
- Restaurant: http://localhost:8082/swagger-ui.html
- Order: http://localhost:8083/swagger-ui.html
- Payment: http://localhost:8084/swagger-ui.html

These hit the service directly (not through the gateway), so for anything
other than `POST /auth/register`, `POST /auth/login` or `GET /restaurants*`
you'll need to click "Authorize" in Swagger's UI and paste in a JWT you
got from a login/register call first.

## Automated unit tests

```bash
cd auth-service        && mvn test
cd restaurant-service   && mvn test
cd order-service        && mvn test
cd payment-service       && mvn test
```

What's covered:

| Service | Key scenarios tested |
|---|---|
| auth-service | Successful registration, duplicate-email rejection, successful login, wrong-password rejection |
| restaurant-service | Restaurant creation, non-owner update rejection, adding a menu item to a nonexistent restaurant, rating average recomputation |
| order-service | Payment approved → `PAYMENT_COMPLETED`, payment declined → `PAYMENT_FAILED`, closed-restaurant rejection, non-owner accept rejection, accepting an unpaid order rejection, reject-triggers-refund, cancel-after-acceptance rejection, rating a delivered order, rejecting a rating on a non-delivered or already-rated order, notification emails never throw even when auth-service or the mail server is unreachable |
| payment-service | Successful charge, declined charge, idempotent re-processing of the same order, refund-on-non-success rejection, refund-of-nonexistent-payment rejection |

Surge pricing tests use a fixed `Clock` (10:00 AM UTC, outside both peak
windows) rather than the real system clock, so they give the same result
no matter what time of day you actually run `mvn test`.

`eureka-server` and `api-gateway` have no business logic of their own (they
route/register), so there are no unit tests for them — you verify those by
checking the Eureka dashboard (:8761) shows every service registered, and
that requests through :8080 are actually reaching each backend correctly,
which the Postman collection exercises end to end.
