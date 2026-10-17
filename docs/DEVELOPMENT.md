# Development Guide

## Repository Setup

### Prerequisites

- Git
- For Android development: Android Studio, JDK 17+
- For Backend development: a PHP release supported by the selected Laravel stable release (PHP 8.4 as selected), MySQL 8.x reachable over the network (hosted, not local), and Composer
- For Admin development: Node.js LTS and npm

### Clone and Setup

```bash
git clone https://github.com/Averon-Labs/eshop-verse.git
cd eshop-verse
```

### Component-Specific Setup

Each component has its own setup process. The source code and actual dependency versions remain authoritative. See:

- `android/` — Java/XML Android application in Android Studio
- `backend/` — Laravel API; configure the local environment from `.env.example`, migrate the development database, and run tests against a separate disposable test database (see the Backend section)
- `admin/` — React/TypeScript client; configure its API origin without committing credentials

Before a component is implemented, document its exact setup, build, lint, and test commands here. Do not invent commands in task prompts; copy them from the current project scripts/tooling.

---

## Conventions

All branching, commit, PR, testing, and agent workflow conventions are defined in [`AGENTS.md`](../AGENTS.md). Do not duplicate them here.

---

## Local Development

### Android

**Prerequisites:** JDK 17 or later, Android SDK Platform 36.1 (API 36 extension level 1), and Android SDK Build Tools 36.0.0. The Gradle Wrapper files in `android/gradle/wrapper/` must be present in the checkout.

**Android commands:**

From PowerShell at the repository root:

```powershell
Set-Location android
.\gradlew.bat --version
.\gradlew.bat clean assembleDebug
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
```

For a connected-device instrumentation run, use `.\gradlew.bat connectedDebugAndroidTest` from `android/` with an emulator or device available. This requires a running emulator or physical device and is not included in CI.

To use Android Studio, open the `android/` directory and sync Gradle. Run the app on an emulator or device after the local checks pass.

**Current Configuration:**
- Language: Java/XML
- Namespace and application ID: `com.averonlabs.eshopverse`
- Compile SDK: 36.1 (API 36 extension level 1)
- Min SDK: 25
- Target SDK: 36
- Java source/target: 11
- Android Gradle Plugin: 9.2.1
- Gradle wrapper: 9.4.1

**Dependencies** (Phase 0-2 complete set declared in FND-006):
- Material Components: 1.14.0
- AndroidX AppCompat: 1.8.0
- AndroidX Activity: 1.13.0
- AndroidX Core: 1.18.0 (compatible with Compile SDK 36.1)
- ConstraintLayout: 2.2.2
- Navigation: 2.10.2
- Lifecycle: 2.11.0
- AndroidX Fragment: 1.9.1
- RecyclerView: 1.4.0
- SwipeRefreshLayout: 1.2.0
- Retrofit: 3.0.0 (with Gson converter)
- OkHttp: 5.3.0 (with logging interceptor and MockWebServer)
- Glide: 4.16.0 (compatible with Compile SDK 36.1)
- Testing: JUnit 4.13.2, Mockito 5.24.0, AndroidX Test JUnit 1.3.0, Espresso 3.7.0

**Verification status (2026-10-10):** local prerequisites include JDK 25, Android SDK Platform
36.1, and Build Tools 36.0.0. The owner chose to retain Compile SDK 36.1, so AndroidX Core 1.18.0
and Glide 4.16.0 are used instead of releases requiring API 37. From `android/`, the following
Windows command completed successfully (`BUILD SUCCESSFUL`, Gradle 9.4.1):

```powershell
.\gradlew.bat --no-daemon clean assembleDebug lintDebug testDebugUnitTest
```

The CI workflow declares the equivalent Linux commands (`./gradlew --no-daemon clean
assembleDebug`, `./gradlew --no-daemon lintDebug`, and `./gradlew --no-daemon testDebugUnitTest`)
on JDK 17, but a hosted CI run has not been observed yet. Instrumentation tests still require a
connected device/emulator and were not run. Gradle reports deprecated features that will be
incompatible with Gradle 10; the current Gradle 9.4.1 tasks pass. The executable bit of
`android/gradlew` in the Git index must be confirmed by the repository owner because Android-task
agents do not run Git commands.

All Android UI is Java/XML using the shared Material 3 design system in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md). Use the common app theme and shared resource tokens for each screen.

The launcher currently contains a clean application host (`HomeFragment`). Product screens and behavior are not implemented yet. The Gradle Wrapper JAR is tracked in Git.

**Known Limitation:** Release builds use `isMinifyEnabled = false` (R8/ProGuard disabled for the demo). This is acceptable for the first release but should be enabled for production.

Android Phase 0 also adds the layers the screen tasks depend on; their prerequisites are in [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §B, and the per-task brief format is in [`android/TASK-BRIEF-TEMPLATE.md`](android/TASK-BRIEF-TEMPLATE.md).

### Backend

**Selected stack (roadmap task FND-002):**

| Component | Selected version / choice |
|-----------|---------------------------|
| PHP | **8.4.25** CLI (installed via winget `PHP.PHP.8.4`); extensions enabled in `php.ini`: `curl`, `fileinfo`, `mbstring`, `openssl`, `pdo_mysql`, `mysqli`, `zip`, plus the framework defaults (`ctype`, `dom`, `json`, `tokenizer`, `xml`, `PDO`) |
| Composer | **2.10.3** |
| Framework | **Laravel 13** — skeleton `laravel/laravel` v13.11.0, locked `laravel/framework` **v13.35.0** in `backend/composer.lock` |
| Test runner | **PHPUnit 12.5.38** through `php artisan test` |
| Style/static check | **Laravel Pint 1.32.1** (PSR-12 + Laravel preset), no `pint.json` overrides |
| Database | **MySQL 8.4** (`utf8mb4`, `utf8mb4_unicode_ci`, strict mode) — local zip-based MySQL 8.4.0 on `127.0.0.1:3306` (no admin rights, no Windows service). Two databases on the same server: `eshopverse_dev` for development and `eshopverse_test` as the disposable test database. A hosted MySQL 8+ service over TLS replaces it by filling the same `DB_*` values in `.env` |
| Node.js / Vite | Not required for API work. `package.json` and `vite.config.js` exist in the skeleton but only build frontend assets; no backend command below depends on Node |

**Prerequisites:** PHP 8.4 with the extensions above on `PATH`, Composer 2 on `PATH`, and a reachable MySQL 8.0+ server with the two databases below. The verified workflow is the local zip-based MySQL 8.4 described under *Local MySQL setup* below.

```sql
CREATE DATABASE eshopverse_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE eshopverse_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- grant the application user ALL privileges on both databases only.
```

**Local MySQL setup (verified 2026-10-12):** the documented development database is a local
zip-based MySQL 8.4 — it needs no admin rights and no Windows service:

1. Download the MySQL 8.x community archive (`mysql-8.x-winx64.zip`) and extract it anywhere
   (example: `C:\tools\mysql-8.4`).
2. `bin\mysqld.exe --no-defaults --initialize-insecure --basedir=<dir> --datadir=<dir>\data`
3. Start it: `bin\mysqld.exe --no-defaults --basedir=<dir> --datadir=<dir>\data --port=3306 --bind-address=127.0.0.1`
4. Create the two databases and one application user granted on both databases only (SQL above);
   store the generated password only in the git-ignored `.env`.
5. Point `.env` at `DB_HOST=127.0.0.1`, `DB_PORT=3306`, `DB_DATABASE=eshopverse_dev`.

A hosted MySQL 8+ service is a drop-in replacement: fill the same `DB_*` values in `.env` and add
`MYSQL_ATTR_SSL_CA` (see *TLS* below). Do not use a hosted service older than MySQL 8.0.

**Backend commands** (PowerShell, run from `backend/`):

```powershell
# Install dependencies (uses the committed composer.lock)
composer install

# First-time setup
Copy-Item .env.example .env     # then fill DB_HOST / DB_PORT / DB_DATABASE / DB_USERNAME / DB_PASSWORD (MYSQL_ATTR_SSL_CA only for hosted TLS)
php artisan key:generate

# Migrations (development database from .env)
php artisan migrate
php artisan migrate:fresh --seed    # safe reset of development data

# Style / static check
vendor\bin\pint --test    # check only (CI-style)
vendor\bin\pint           # fix in place

# Tests — always run against the disposable eshopverse_test database
php artisan test
php artisan test --filter=DatabaseWorkflowTest

# Manual reset of the test database (only when needed)
$env:DB_DATABASE = 'eshopverse_test'; php artisan migrate:fresh

# Run the API locally
php artisan serve        # http://127.0.0.1:8000  (health: GET /up)
```

**Safety rules**
- `.env` is git-ignored and holds the only real credentials; `.env.example` contains placeholders only. Never commit a `.env`.
- Tests are hard-wired in `phpunit.xml` to `DB_CONNECTION=mysql` and `DB_DATABASE=eshopverse_test`. Never point them at `eshopverse_dev`.
- `RefreshDatabase` wraps each test in a transaction after a one-time `migrate:fresh`, so test data never survives a run.

**Verification status (re-verified 2026-10-14, QUAL-005 clean-checkout rehearsal)** — executed on the committed backend tree, output recorded:

| Command | Result |
|---------|--------|
| `composer install` | ✔ exit 0 — packages resolved from the committed `composer.lock` |
| setup (`Copy-Item .env.example .env`, `php artisan key:generate`) | ✔ `.env` created from `.env.example` (placeholders only); `key:generate` reported `Application key set successfully`; no secret is checked in |
| `composer validate --strict` | ✔ `./composer.json is valid` |
| `composer audit` | ✔ `No security vulnerability advisories found` |
| `php artisan migrate` / `php artisan migrate:fresh --seed` | ✔ all 15 migrations ran on local MySQL **8.4.0**; seeding produced 2 users (1 admin), 5 categories, 12 products (10 active), 12 inventories |
| `php artisan test` | ✔ 153 tests, 2062 assertions, 0 failures (runs on `eshopverse_test`) |
| `vendor\bin\pint --test` | ✔ `{"result":"passed"}` |
| `php artisan serve` | ✔ `GET /up` → 200 |

The OpenAPI contract (`docs/openapi.yaml`) was **not modified** in Phase 3, so the accepted Redocly lint baseline recorded under *OpenAPI contract lint (FND-004)* — 0 errors, 2 intentional warnings — still holds unchanged.

The development database for this run is the **local zip-based MySQL 8.4** workflow described above. Host and credentials live only in `.env`.

**TLS:** `docs/SECURITY.md` requires encrypted database connections where the hosting provider
supports it. The local workflow binds to `127.0.0.1` loopback only — traffic never leaves the
machine, so no TLS is involved. When a hosted MySQL 8+ service replaces it:

- Download `ca.pem` from the MySQL service console (the development copy lives outside the repository).
- Set `MYSQL_ATTR_SSL_CA=<path to ca.pem>` in `.env`; `config/database.php` then enables TLS and
  sets `MYSQL_ATTR_SSL_VERIFY_SERVER_CERT`, so an unauthenticated connection fails instead of
  silently falling back to plaintext.
- Never use a hosted MySQL older than 8.0, or one that offers no TLS, for development credentials.

> The OpenAPI contract lives at [`openapi.yaml`](openapi.yaml); it was reviewed and accepted by the API owner on 2026-10-12 (FND-004). Implement endpoints, client integration, and business migrations only as it describes, and change it first through the contract-change process.

### OpenAPI contract lint (FND-004)

The contract is validated with the pinned Redocly CLI (the toolchain decision for FND-004; Node.js 24 is already required for the Android toolchain):

```bash
npx --yes "@redocly/cli@2.62.1" lint docs/openapi.yaml
```

Verified output (2026-10-12, exit code 0):

```text
No configurations were provided -- using built in recommended configuration by default.

validating docs\openapi.yaml...
docs\openapi.yaml:
  46:10  warning  no-server-example.com  Server `url` should not point to example.com or localhost.
  48:10  warning  no-server-example.com  Server `url` should not point to example.com or localhost.

docs\openapi.yaml: validated in 400ms

Woohoo! Your API description is valid. 🎉
You have 2 warnings.
```

Result: **0 errors, 2 warnings, exit 0.** Both warnings are intentional, understood, and accepted:

- `46:10` — `http://localhost:8000/api/v1`, the local Laravel development server.
- `48:10` — `http://localhost:8081/api/v1`, the local contract mock used by Android development before the API exists.

They document local development servers on purpose; the production-like origin is the separately labelled `https://api.example.test/api/v1` placeholder whose replacement is recorded as decision OPEN-1 (closed 2026-10-11). These two warnings are the complete accepted baseline: any new warning or error is a regression and must be fixed.

**Do not add a `redocly.yaml` (or any Redocly configuration file) to silence, downgrade, or exclude rules.** The warnings stay visible as living documentation of the accepted state.

### Admin Dashboard

**Selected stack (roadmap task FND-003):**

| Component | Selected version / choice |
|-----------|---------------------------|
| Runtime | **Node.js 24.17.0** with **npm 11.17.0** (Vite 8 requires Node `^20.19.0 \|\| >=22.12.0`) |
| UI | **React 19.3.1** + **React DOM 19.3.1** (`@types/react` / `@types/react-dom` 19.3.0) |
| Styling | **Tailwind CSS 4.3.3** with `@tailwindcss/vite` 4.3.3 — CSS-first configuration in `src/globals.css` (`@theme inline` tokens, `@custom-variant dark`); no `tailwind.config.ts`. Tailwind v3 was rejected during AUTH-003 because its toolchain carried unfixed npm audit advisories |
| Routing | **react-router-dom 7.18.4** — `/login` plus the authenticated shell routes `/products`, `/products/new`, `/products/:productId/edit`, `/categories`, and `/inventory` |
| Language | **TypeScript 6.0.3** (`strict`, `noEmit` type-check) |
| Build/dev server | **Vite 8.3.4** with `@vitejs/plugin-react` 6.1.2 |
| Tests | **Vitest 5.0.5** (jsdom environment) + **React Testing Library 16.3.3** + `@testing-library/dom` 10.4.2 + `@testing-library/jest-dom` 7.0.1, `src/setupTests.ts` |
| Static check | **ESLint 10.12.0** flat config (`eslint.config.js`) with `@eslint/js` and **typescript-eslint 8.71.2** |
| Package manager | **npm** — `admin/package-lock.json` is committed; install with `npm ci` |

**Prerequisites:** Node.js 20.19+ (22.12+) and npm on `PATH`.

**Admin commands** (run from `admin/`):

```bash
# Clean dependency install from the committed package-lock.json
npm ci

# First-time local configuration (optional — the default origin is used when absent)
cp .env.example .env.local     # PowerShell: Copy-Item .env.example .env.local

# Development server (forced to http://localhost:5173)
npm run dev

# Type-check / static check
npm run typecheck              # tsc --noEmit
npm run lint                   # eslint .

# Tests (Vitest + React Testing Library, jsdom)
npm test
npm run test:watch

# Production build (runs the type-check first, output in dist/)
npm run build
npm run preview                # serve the production build locally
```

**Local SPA/API origin arrangement (Sanctum cookies and CSRF):**

- Open the SPA as `http://localhost:5173` and point `VITE_API_BASE_URL` at a `localhost` API
  origin — do not mix `localhost` and `127.0.0.1`, because the `SameSite=Lax` session cookie is
  only sent within one site.
- The Vite dev server is pinned to `http://localhost:5173` (`strictPort`). The backend's
  `SANCTUM_STATEFUL_DOMAINS` already lists `localhost:5173` and `localhost:5174`, so the stateful
  `eshop_admin_session` cookie is accepted for the SPA origin.
- Cross-origin cookie/CSRF traffic is accepted only for the documented local origins: the
  backend's `config/cors.php` allows `http://localhost:5173` and `http://localhost:5174` with
  `supports_credentials: true` (the `CORS_ALLOWED_ORIGINS` variable overrides the list). Requests
  from any other origin receive no CORS approval.
- The API origin comes from `VITE_API_BASE_URL` (template: `admin/.env.example`; local override:
  `.env.local`, git-ignored). Default: `http://localhost:8000/api/v1`. No credential, key, or
  secret belongs in this file or anywhere in the admin source.
- Requests that carry session/CSRF state (login, logout, mutations) are sent with
  `credentials: 'include'` so `eshop_admin_session` and `XSRF-TOKEN` cookies travel, and echo the
  `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header. Tokens are never written to `localStorage`;
  the SPA derives its auth state from `GET /admin/auth/session`.
- If port 8000 is occupied locally, run the API on another port (for example
  `php artisan serve --port=8010`) and update `VITE_API_BASE_URL` accordingly.

**Verification status (2026-10-16, AUTH-003 + CAT-004 on the admin SPA):**

| Command (from `admin/`) | Result |
|-------------------------|--------|
| `npm ci` + `npm audit` | ✔ install clean; `npm audit` reports 0 vulnerabilities after the Tailwind v4 migration |
| `npm run typecheck` | ✔ exit 0, no errors |
| `npm run lint` | ✔ exit 0, no warnings or errors |
| `npm test` | ✔ 8 test files, 58 tests passed (session lifecycle, products, categories, inventory) |
| `npm run build` | ✔ exit 0 — `dist/` with one CSS (~35 kB) and one JS (~326 kB) bundle |
| `npm run dev` + `GET http://localhost:5173/` | ✔ HTTP 200, Vite dev server on port 5173 |

Vitest runs without globals, so `src/setupTests.ts` calls `afterEach(cleanup)` explicitly to keep
React Testing Library renders isolated between tests. The Vitest config caps parallel workers and
raises the per-test timeout for slow handset hardware, and `setupTests.ts` sets RTL's
`asyncUtilTimeout` to 5000 ms; these only extend waits for genuinely slow environments, they do not
weaken assertions.

**Verification status (2026-10-17, ORD-003 on the admin SPA):**

| Command (from `admin/`) | Result |
|-------------------------|--------|
| `npm run typecheck` | ✔ exit 0, no errors |
| `npm run lint` | ✔ exit 0, no warnings or errors |
| `npm test` | ✔ 10 test files, 82 tests passed (adds order list/detail behavior tests and the load-sensitivity fixes) |
| `npm run build` | ✔ exit 0 — `dist/` with one CSS (~36 kB) and one JS (~345 kB) bundle |
| `php artisan test --filter="AdminOrder|Order"` (from `backend/`) | ✔ 41 tests, 719 assertions, 0 failures |

On this Android/Termux host, **PHP 8.5.1's built-in server can fail to start with
`Cannot create lock - Permission denied (13)`** (including via `php artisan serve`, and even with
`php -n`). Start it with opcache disabled from a writable working directory instead:

```bash
php -d opcache.enable=0 -d opcache.enable_cli=0 -S 127.0.0.1:8000 -t /abs/path/to/backend/public
```

Live admin order probes (disposable dev data) confirmed the session handshake, guest `401`s,
list filters/pagination, `409 INVALID_TRANSITION` for invalid moves, and the documented
`confirmed → processing → shipped → delivered` and `pending_payment → cancelled` transitions;
details are recorded in the ORD-003 verification record in `docs/ADMIN_ROADMAP.md`.

---

## Task and Agent Guidance

Follow [`AGENTS.md`](../AGENTS.md) for roadmap task selection, clarification, and agent-role assignment; it links the concise role guides. Use this document for component setup and verified commands. CI should run the required build, lint, unit, and integration/API checks; verify the checked-in workflow and repository settings before claiming they are active.
