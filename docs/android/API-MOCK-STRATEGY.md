# Android API Mock Strategy

## Why this exists

Every Android task in Phases 1 and 2 consumes a backend endpoint whose canonical implementation
already depends on backend tasks (FND-004, AUTH-001, CAT-001, CART-001, ORD-001). Without a
substitute, no Android screen can be built, tested, or demonstrated until the whole backend exists,
and every Android acceptance criterion that says "uses the reviewed API contract" becomes
unverifiable.

This strategy removes that block: Android work is verified against
[`docs/openapi.yaml`](../openapi.yaml), which is the same contract the backend will implement. The
contract, not the running backend, is the interface Android depends on.

**Boundary:** the mock proves the Android client matches the contract. It never proves the backend
implements the contract. Backend verification stays in the backend roadmap.

---

## Layers

| Layer | Purpose | Runs where |
|-------|---------|-----------|
| **Contract** | `docs/openapi.yaml` — schemas, status codes, error codes, state machine | Repository |
| **Fixtures** | Canonical JSON responses and error bodies, one file per operation and scenario | `android/app/src/test/resources/fixtures/api/` |
| **Unit-test transport** | OkHttp `MockWebServer` replaying fixtures | JVM unit tests (`testDebugUnitTest`) |
| **Local mock server** | A dependency-free router over the same fixtures with in-memory mutable state | Developer machine, reached by the emulator as `http://10.0.2.2:8081/api/v1` |
| **Real backend** | Laravel, when AUTH-001/CAT-001/CART-001/ORD-001 land | Same base-URL override |

There is exactly **one** fixture directory. Unit tests read it from the test classpath; the mock server
reads the same files from disk. A fixture is never copied.

---

## Fixture conventions

```
android/app/src/test/resources/fixtures/api/<operationId>/<scenario>.json
```

- `<operationId>` is the exact `operationId` from `docs/openapi.yaml` (`loginCustomer`,
  `getProduct`, `submitSimulatedPayment`, ...).
- `<scenario>` is a short lowercase slug: `success`, `empty`, `validation-error`,
  `insufficient-stock`, `unauthenticated`, `invalid-credentials`, `not-found`, `rate-limited`,
  `payment-failure`, `idempotent-replay`.
- Error fixtures contain the **complete** documented envelope, including `error.code`, because the
  client maps behaviour from `error.code`, never from the message text.
- Money is always a decimal string in fixtures. A fixture containing a JSON number for a money field
  is a defect.
- A fixture is contract-conformant only if its shape and status code match `docs/openapi.yaml`. The
  task that adds a fixture states which operation and response it mirrors.
- Fixtures contain no real personal data, no token that could work anywhere, and no card data.

**Status codes live in the test, not the file.** A fixture directory contains only bodies; the test
declares `200`, `409`, and so on. Use a sidecar `_status` key only when one operation needs several
statuses in the same directory — `getProduct/not-found.json` with `{"_status": 404, "error": {...}}`
— and have the loader strip `_status` before serving.

---

## Local mock server

- Location: `android/tools/mock-api/` — inside the Android component, because it exists to unblock
  Android work and must not become a second backend.
- Default fixtures directory: `android/app/src/test/resources/fixtures/api`.
- Default bind: `127.0.0.1:8081`, overridable with `--port`. It must **not** bind `0.0.0.0` by
  default.
- **State:** the mock holds cart, order, and inventory state in memory for one process lifetime, seeded
  from fixtures. Without state, the cart and checkout flows cannot be exercised end to end.
- **Control endpoints** (prefixed `/__mock/`, never part of the product contract):
  - `POST /__mock/reset` — restore the seed state; call it before each scenario.
  - `GET /__mock/state` — inspection for debugging a failing scenario.
- **Behavioural requirements:** it must return the documented status code and error envelope for
  every failure path the client is expected to handle (validation, 401, 404, 409, 429), and it must
  honour `Idempotency-Key` on `POST /orders` and `POST /orders/{orderId}/payment` so duplicate-submit
  behaviour is testable before the backend exists.
- **Deliberate limitation:** it is single-user and unauthenticated. Ownership tests belong to the
  backend suite. The Android client's 401 handling is exercised with a fixture-driven scenario, not
  by the mock refusing a token.
- **Runner:** a dependency-free router over the fixtures. Recommended runner is Python 3 stdlib
  (`python tools/mock-api/server.py`), because it requires no install on the current development
  machine. A Node runner (`server.mjs`) is an alternative once the admin toolchain exists. Only one
  runner is committed; the fixtures are runner-independent. **This choice is OPEN-5** and needs an
  owner decision before NET-002 closes.

Emulator networking: `10.0.2.2` is the host loopback from inside the Android emulator. Cleartext to
that host is permitted only in the `debug` build through
`app/src/debug/res/xml/network_security_config.xml`; the `release` build must not permit cleartext.

---

## Switching to the real backend

The base URL is a build-time field, not a constant in code:

| Build | Base URL | Source |
|-------|----------|--------|
| `debug` (default) | `https://eshopverse.wasmer.app/api/v1` | hosted demo backend |
| `debug` (local mock) | `http://10.0.2.2:8081/api/v1` via `-PapiBaseUrl=` | mock server |
| `debug` (local Laravel) | `http://10.0.2.2:8000/api/v1` via `-PapiBaseUrl=` | developer machine |
| `release` | `https://eshopverse.wasmer.app/api/v1` | hosted demo backend |

Switching is a base-URL change plus a `POST /__mock/reset` when the local mock is used. No production
code changes, because nothing in the app knows whether the server is the mock or Laravel.

The in-app `OfflineMockInterceptor`, when enabled for catalog/cart/order demos, never answers
`/auth/*`. Registration, sign-in, Google sign-in, profile reads, and password reset always go to the
real API, so an unreachable backend surfaces as a network error instead of a fabricated demo account.

When the real backend exists, the same Android test suite runs against it by overriding the base URL
and skipping the mock control calls. That is the integration check that closes the contract loop; it
belongs to the integration/regression work (QUAL-001), not to the individual feature tasks.

---

## What the mock must not become

- Not a place to design new endpoints. If a screen needs an operation the contract lacks, the answer
  is a contract change reviewed by the API owner, not a mock-only route.
- Not a fixture set that drifts. If a fixture and `docs/openapi.yaml` disagree, the fixture is wrong;
  fix it in the same commit that fixes the client.
- Not a replacement for backend tests. Mock-green is never reported as backend-green.
