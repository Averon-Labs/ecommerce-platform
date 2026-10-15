# Backend API — Agent Instructions

> Read the root `AGENTS.md` first. This file contains backend-specific rules.

The canonical Backend task steps and acceptance criteria are in [`docs/BACKEND_ROADMAP.md`](../docs/BACKEND_ROADMAP.md). Project gates and shared tasks are in [`docs/ROADMAP.md`](../docs/ROADMAP.md).

---

## Architecture

| Aspect | Decision | Status |
|--------|----------|--------|
| Language | PHP | DECIDED |
| API Style | RESTful | DECIDED |
| Database | MySQL | DECIDED |
| Framework | Laravel, supported stable release selected at project initialization | DECIDED |
| Authentication | Laravel Sanctum | DECIDED |
| Migration Tool | Laravel migrations | DECIDED |

---

## Project Structure

Follow the conventional structure of the selected Laravel skeleton. Keep migrations and seeders under `database/`, API routes in the framework's API route file, and tests in the framework-supported `tests/Feature` and `tests/Unit` locations. The checked-in source and Composer configuration are authoritative.

---

## Conventions

### API

- Follow the API contract in `docs/API_CONTRACT.md`.
- Update/review `docs/openapi.yaml` before implementing or changing endpoints; keep client and server work on the same contract.
- Use JSON for all request and response bodies.
- Return appropriate HTTP status codes.
- Include consistent error responses.
- Validate all input server-side.
- Enforce ownership and role authorization server-side for every resource/action.

### Code Style

- Follow PSR-12 coding standards.
- Use type declarations for parameters and return types.
- Use meaningful names for classes, methods, and variables.
- Document public methods with PHPDoc.

### Security

- Use parameterized queries — never concatenate SQL.
- Hash passwords with bcrypt or Argon2.
- Validate and sanitize all user input.
- Never expose internal errors to clients.
- See `docs/SECURITY.md` for complete guidelines.

---

## Boundaries

- Backend code lives exclusively in `backend/`.
- The API is the only interface for client applications.
- Do not serve frontend assets from the API (separate concerns).
- Database access is only through the backend.

---

## Testing

- Unit tests in `tests/Unit/`
- HTTP/API and integration tests in `tests/Feature/`
- See `docs/TESTING.md` for conventions.
- Use PHPUnit/Laravel tests and isolated test data; never run test migrations against a development or production database.
