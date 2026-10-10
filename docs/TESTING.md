# Testing Strategy

## Purpose

This document defines the testing strategy and conventions for the EShop Verse platform. Agent-stage procedures live in the role guides linked from the root [`AGENTS.md`](../AGENTS.md).

> **Note:** This is the required strategy. Exact local and CI commands must be added to `docs/DEVELOPMENT.md` when each component's test runner is configured.

---

## Testing Levels

### Unit Testing

**Android:**
- Test ViewModels, Repositories, and utility classes.
- Use the scaffold's configured JUnit 4 runner; change it only with a documented toolchain decision.
- Use Mockito for mocking dependencies.
- Unit tests run without Android framework dependencies.

**Backend (PHP):**
- Test service classes, validators, and data transformations.
- Use PHPUnit with Laravel's test support.
- Mock database and external service interactions.

**Admin (React/TypeScript):**
- Use Vitest and React Testing Library to test user-visible components and behavior.
- Keep tests focused on application behavior rather than framework internals.

### Integration Testing

**Android:**
- Test Room database operations.
- Test API client with mock server responses.
- Use AndroidX Test libraries.

**Backend (PHP):**
- Test API endpoints with a test database.
- Verify database queries and transactions.
- Test authentication and authorization flows.

### API Testing

- Test all API endpoints against the contract in `API_CONTRACT.md`.
- Verify request validation and error responses.
- Test authentication and authorization.
- Test pagination and filtering.
- Can be automated with tools like Postman/Newman or REST Assured.

### UI Testing

**Android:**
- Test critical user flows (login, browse, add to cart, checkout).
- Use Espresso for UI testing.
- Focus on happy paths and critical error paths.

### Regression Testing

- All existing tests must pass before merging.
- CI pipeline runs the full test suite on every PR.
- No tests should be disabled without documented justification.

### Test Balance and Priority

- Keep many fast unit tests, fewer integration/API tests, and a small number of end-to-end UI smoke tests.
- Prioritize money calculations, stock/order transactions, validation, authentication, authorization, and error handling.
- Do not use a coverage percentage as the sole measure of quality. Tests must cover acceptance criteria, important boundaries, and failure paths.
- Use isolated, disposable test data. Never point automated tests at development or production databases.
- API tests must verify that customers cannot read or change another customer's cart, profile, or orders, and that customer accounts cannot call admin operations.
- Checkout tests must cover duplicate submission and simulated payment success/failure; they must never contact or charge a real payment provider.

---

## Test Naming Conventions

### Android (Java)

```java
@Test
void methodName_scenario_expectedBehavior() { }

// Examples:
@Test
void getProducts_emptyCategory_returnsEmptyList() { }

@Test
void addToCart_invalidQuantity_throwsException() { }
```

### Backend (PHP)

```php
public function test_method_scenario_expected_behavior(): void { }

// Examples:
public function test_create_user_duplicate_email_returns_409(): void { }

public function test_get_products_with_pagination_returns_paged_results(): void { }
```

Method names are written in `snake_case` because the project style check
(Laravel Pint, `vendor/bin/pint --test`) enforces it; run `vendor/bin/pint`
to fix casing and formatting automatically.

---

## Test Organization

### Android

```
android/app/src/test/          # Unit tests
android/app/src/androidTest/   # Instrumented tests
```

### Backend

```
backend/tests/
├── Unit/       # Isolated application logic
└── Feature/    # Laravel HTTP, API, database, and integration tests
```

---

## Definition of Done (Testing)

- [ ] New code has unit tests covering core logic.
- [ ] Edge cases and error paths are tested.
- [ ] All existing tests pass.
- [ ] No tests are skipped or disabled without documented reason.
- [ ] Test names follow the naming convention.
- [ ] Tests are deterministic (no flaky tests).
- [ ] Integration tests use isolated test data.
- [ ] API contract tests agree with the reviewed OpenAPI description when it is added.
- [ ] After a fix, a regression test fails before the fix and passes after it.
- [ ] Required CI checks pass before merge.
