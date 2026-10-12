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
- `backend/` — Laravel API; configure the local environment from `.env.example`, migrate the hosted development database, and run tests against a separate disposable test database (see the Backend section)
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
`android/gradlew` in the Git index must be confirmed (`git ls-files -s android/gradlew` shows
mode 100755); Android agents manage Git on phase branches per `AGENTS.md`.

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
| Database | **Hosted MySQL** (`utf8mb4`, `utf8mb4_unicode_ci`, strict mode) — no local MySQL. Two databases on the same server: `eshopverse_dev` for development and `eshopverse_test` as the disposable test database |
| Node.js / Vite | Not required for API work. `package.json` and `vite.config.js` exist in the skeleton but only build frontend assets; no backend command below depends on Node |

**Prerequisites:** PHP 8.4 with the extensions above on `PATH`, Composer 2 on `PATH`, and a reachable hosted MySQL server with the two databases below.

```sql
CREATE DATABASE eshopverse_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE eshopverse_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- grant the application user ALL privileges on both databases only.
```

**Backend commands** (PowerShell, run from `backend/`):

```powershell
# Install dependencies (uses the committed composer.lock)
composer install

# First-time setup
Copy-Item .env.example .env     # then fill DB_HOST / DB_PORT / DB_DATABASE / DB_USERNAME / DB_PASSWORD / MYSQL_ATTR_SSL_CA
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

**Verification status (2026-10-11)** — executed on this checkout, output recorded:

| Command | Result |
|---------|--------|
| `composer install` | ✔ exit 0 — 79 packages from `composer.lock` |
| setup (`Copy-Item .env.example .env`, `php artisan key:generate`) | ✔ the skeleton's `post-root-package-install` created `.env` from `.env.example` and `key:generate` reported `Application key set successfully`; no secret is checked in |
| `composer validate --strict` | ✔ `./composer.json is valid` |
| `composer audit` | ✔ `No security vulnerability advisories found` |
| `vendor\bin\pint --test` | ✔ `{"result":"passed"}` |
| `php artisan migrate` | ✔ 3 migrations ran on the hosted `eshopverse_dev` (MySQL **8.4.11**) |
| `php artisan migrate:status` | ✔ all 3 `[1] Ran` |
| `php artisan test` | ✔ 5 tests, 8 assertions, 0 failures (runs on `eshopverse_test`) |
| `php artisan serve` | ✔ `GET /up` → 200, `GET /api/v1/definitely-not-a-route` → 404 |
| TLS connection | ✔ TLS 1.3, `TLS_AES_256_GCM_SHA384`, certificate verified against `MYSQL_ATTR_SSL_CA` |

The development database used for this run is a **hosted MySQL service (Aiven Free tier)** —
remote, no local MySQL. Host and credentials live only in `.env`.

**TLS (verified, 2026-10-11):** `docs/SECURITY.md` requires encrypted database connections, so the
hosted connection runs over **TLS 1.3** with cipher `TLS_AES_256_GCM_SHA384` and the server
certificate is **verified** against the provider's project CA:

- Download `ca.pem` from the MySQL service console (the development copy lives outside the repository).
- Set `MYSQL_ATTR_SSL_CA=<path to ca.pem>` in `.env`; `config/database.php` then enables TLS and
  sets `MYSQL_ATTR_SSL_VERIFY_SERVER_CERT`, so an unencrypted or unauthenticated connection fails
  instead of silently falling back to plaintext.
- Without `MYSQL_ATTR_SSL_CA` the connection is plaintext. That is intentionally loud in this
  document: never run migrations or tests in that state.

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

1. Install the pinned Node.js/npm dependencies.
2. Configure the API origin for the local Laravel server.
3. Run the documented development, lint, and test commands.

> Exact commands will be documented when the React application and test configuration are initialized.

---

## Task and Agent Guidance

Follow [`AGENTS.md`](../AGENTS.md) for roadmap task selection, clarification, and agent-role assignment; it links the concise role guides. Use this document for component setup and verified commands. CI should run the required build, lint, unit, and integration/API checks; verify the checked-in workflow and repository settings before claiming they are active.
