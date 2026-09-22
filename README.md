# Resilience4j Payment Circuit Breaker Demo

Minimal Spring Boot project that reproduces the presentation's scenario end to end:
a booking platform calls a third-party payment gateway; when that gateway gets
slow or starts failing, a Resilience4j `CircuitBreaker` trips and starts failing
fast instead of letting requests pile up and exhaust the server's thread pool.

## What's in here

| File | Purpose |
|---|---|
| `PaymentGatewayClient` | Stands in for the real third-party gateway. Normally instant; can be switched into a "slow/failing" mode on demand. |
| `PaymentService` | Wraps the gateway call with `@CircuitBreaker`, logs every state transition, and falls back to `queuedForRetry(...)` when the breaker is open. |
| `PaymentController` | `POST /api/payments` to charge, `GET /api/payments/circuit-status` to watch the breaker's state. |
| `GatewaySimulatorController` | Demo-only control panel — flips the mock gateway into an outage so you can trigger the breaker live. |
| `application.yml` | The exact thresholds from the presentation (50% failure rate, 10s window, 30s open state, etc). |

## Requirements

- Java 17+
- Maven 3.9+ (or just use the included behaviour with your IDE's Spring Boot run)
- Normal internet access the first time you build, so Maven can download Spring
  Boot / Resilience4j from Maven Central

> This project was scaffolded in a sandboxed environment without access to
> Maven Central, so it could not be compiled there. It was written and
> reviewed carefully against the Resilience4j 2.x / Spring Boot 3.x APIs, but
> **build it once on your own machine before presenting** to catch anything
> environment-specific.

## Run it

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## Demo script (matches the slides)

**1. Happy path — gateway healthy, breaker CLOSED**

```bash
curl -s -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-1","amount":49.90}'
```

You get back `{"orderId":"ORD-1","status":"CONFIRMED", ...}` immediately.

**2. Trigger the outage** — make the mock gateway hang for 4 seconds and then
fail, on every call:

```bash
curl -s -X POST http://localhost:8080/api/simulate/outage \
  -H "Content-Type: application/json" \
  -d '{"enabled":true,"delayMs":4000,"failurePercentage":100}'
```

**3. Send a burst of payments** (5 is enough — that's `minimumNumberOfCalls`
in `application.yml`):

```bash
for i in $(seq 1 6); do
  curl -s -X POST http://localhost:8080/api/payments \
    -H "Content-Type: application/json" \
    -d "{\"orderId\":\"ORD-$i\",\"amount\":10}"
  echo
done
```

The first few calls take ~4 seconds each and come back `QUEUED_FOR_RETRY`
(the gateway failed, so the fallback ran). Watch the console log — once the
failure rate crosses 50%, you'll see:

```
>>> Circuit breaker 'paymentService' changed state: CLOSED -> OPEN
```

From that point on, calls return `QUEUED_FOR_RETRY` **instantly** — no more
4-second waits — because the breaker is short-circuiting the call before it
ever reaches the (still-broken) gateway. This is the exact effect the slides
describe: the payment path degrades gracefully instead of hanging.

Confirm the state directly at any point:

```bash
curl -s http://localhost:8080/api/payments/circuit-status
# {"state":"OPEN"}
```

**4. Recover the gateway:**

```bash
curl -s -X POST http://localhost:8080/api/simulate/recover
```

Wait for `waitDurationInOpenState` (30s), send one more payment, and watch
the log show `OPEN -> HALF_OPEN -> CLOSED` as the trial calls succeed.

**5. (Optional) Actuator view** of the breaker's live metrics:

```bash
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8080/actuator/circuitbreakers | jq
curl -s http://localhost:8080/actuator/circuitbreakerevents | jq
```

## Why these config values (`application.yml`)

These map directly to the scenario in the presentation:

- `failureRateThreshold: 50` — matches "50% failures" from the scenario.
- `slowCallDurationThreshold: 3s` — a call slower than 3s counts as failed,
  so a 30-second real-world hang trips the breaker long before it can tie up
  a thread indefinitely.
- `slidingWindowType: TIME_BASED`, `slidingWindowSize: 10` — evaluates the
  last 10 seconds of calls, matching "50% failures over 10 seconds."
- `waitDurationInOpenState: 30s` — how long the breaker stays OPEN before
  trying a trial call again.
- `server.tomcat.threads.max: 10` — deliberately small, so it's easy to see
  how quickly a hanging gateway would exhaust a real thread pool without the
  breaker in place.
# resilience4j-payment-circuitbreaker-demo
