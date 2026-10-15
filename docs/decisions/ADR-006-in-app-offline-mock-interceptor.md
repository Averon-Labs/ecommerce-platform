# ADR-006: In-App OfflineMockInterceptor for Android Independent Development

## Status

PROPOSED

## Date

2026-10-15

## Context

ADR-005 established a standalone mock API server runner (listening on `localhost:8081`) for independent client development. While effective for manual exploration and web clients, running an external process in CI environments and on physical Android test devices introduces networking complexities (loopback routing via `10.0.2.2`, port collision, process lifecycle management).

To enable reliable, zero-configuration local UI exploration and hermetic JVM unit/instrumentation tests without external runtime dependencies, Android development introduced an in-app `OfflineMockInterceptor`.

## Options Considered

### Option A: Rely solely on external mock server runner (ADR-005)
- **Pros:** Single external mock implementation shared between Android and Admin.
- **Cons:** Flaky in CI; requires Android emulator network routing (`10.0.2.2`); fails on physical devices unless reverse-tethered; requires developers to run and monitor background processes.

### Option B: In-App OkHttp OfflineMockInterceptor alongside ADR-005 (Selected)
- **Pros:** Completely hermetic; executes in-process; instant response with zero network latency; supports offline demonstration and robust instrumentation tests; works seamlessly on both emulators and physical devices.
- **Cons:** Mock fixture JSONs must be bundled in Android test/asset resources.

## Decision

Adopt `OfflineMockInterceptor` as an in-process OkHttp interceptor for the Android application during development, offline demos, and automated testing, while retaining ADR-005 as an optional external runner for cross-component validation.

The interceptor:
1. Intercepts outgoing HTTP calls to the API base URL.
2. Returns deterministic mock responses loaded from packaged JSON fixtures matching `docs/openapi.yaml`.
3. Maintains in-memory state for mutations (e.g. cart modifications and simulated order creation) during a running app session.

## Consequences

- Android UI and instrumentation tests run deterministically without requiring a live backend or external mock server.
- Seamless developer onboarding (clone and run Android app immediately).
- Integration testing with the live backend is deferred to explicit integration gates (QUAL-001).
