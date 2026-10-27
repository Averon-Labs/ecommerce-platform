# Deployment — EShop Verse Public Demo

This document records the optional public deployment of the portfolio demo. It complements
[`DEVELOPMENT.md`](DEVELOPMENT.md), which owns local setup, and [`HANDOFF.md`](HANDOFF.md), which owns the
reviewer walkthrough. No credential, database password, or application key belongs in this file or in the
repository.

## Current status (2026-10-19)

| Item | Status |
|------|--------|
| Host | ByetHost free hosting — `https://eshopverse.byethost8.com` |
| PHP | 8.3 selected in VistaPanel (Laravel 13 requires PHP 8.3+) |
| Database | MySQL database created in VistaPanel; schema and fictional demo data imported through phpMyAdmin |
| Backend | Laravel app deployed under `htdocs/eshopverse_app`; `https://eshopverse.byethost8.com/up` returns HTTP 200 (`{"status":"up"}` with `Accept: application/json`) |
| API | `GET https://eshopverse.byethost8.com/api/v1/products` returns the seeded active products |
| Admin | React SPA served from the same domain; `POST /api/v1/admin/auth/login` returns `200` with the demo administrator |
| Admin sign-in | SPA at `/login`; accounts are provisioned by the backend seed, with no self-registration |
| Theme | Light/dark toggle implemented in the admin SPA, persisted in `localStorage`. The current production build is `assets/index-DwFFOLMM.js` with `assets/index-C8Rh47uc.css`; as of 2026-10-28 the live host still serves the previous admin bundle (`index-BR1LMIVW.js`) until the upload below is run. |
| Backend redeploy | **Required** for the product-image work (backend Phase 5, `CAT-B01`/`CAT-B02`): the API stores uploaded files on the `product-images` disk (`storage/app/product-images`) and serves them through `GET /api/v1/media/product-images/{productId}/{filename}`. No new environment variable and no schema migration; `storage/app/product-images` must exist and be writable. |

The deployment is a **demo**, not a production commerce service. Payment remains simulated, data is fictional,
and the security limitations in [`SECURITY.md`](SECURITY.md) and [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md)
still apply.

## Upload packages (2026-10-28)

The current build is packaged locally under `output/` (generated, `.gitignore`d; the source of truth is
the repository plus `npm run build`). Upload steps and per-package notes: `output/README.md`.

| Package | Size | Goes to |
|---------|------|---------|
| `output/eshopverse_app.zip` (root folder `eshopverse_app/`) | 16.95 MB | `htdocs/` — extracts into the existing application directory |
| `output/eshopverse-admin.zip` (`index.html`, `assets/`, `fonts/` at the root) | 1.08 MB | `htdocs/` — overwrites the SPA files |

Backend upload rules: never overwrite `htdocs/eshopverse_app/.env` (it is not in the archive), keep the
host's `storage/` content — in particular `storage/app/product-images/` — and keep `storage/` and
`bootstrap/cache/` writable. The archive ships `vendor/` because `backend/composer.lock` changed on
2026-10-23 (`config.platform.php` pinned to 8.3.36) and the host cannot run Composer.

### Verification record (2026-10-28)

| Check | Result |
|-------|--------|
| Backend `php artisan test` | 196 tests / 2,362 assertions — 194 pass. The 2 failures exist only off MySQL: `DatabaseWorkflowTest` asserts the `mysql` driver and `AdminCategoryTest`'s duplicate-name case relies on the MySQL column collation. Both pass in the repository's Backend Build & Test job on MySQL 8.4. `vendor/bin/pint --test` passes. |
| Host PHP compatibility | `composer.json` pins `config.platform.php` to 8.3.36, `vendor/composer/platform_check.php` requires PHP ≥ 8.3.0, and no PHP 8.4-only syntax is used — the build runs on the host's PHP 8.3. |
| Admin panel | `npm run typecheck`, `npm run lint`, `npm test` (11 files / 89 tests), `npm run check:contrast` (34/34) and `npm run build` all pass; the build reproduces `index-DwFFOLMM.js` exactly. |
| Live API smoke, host layout | 88/88 checks against a local mirror of this document's `htdocs/` layout (front controller + SPA + `eshopverse_app/`): `/up`, catalog reads and 404s, registration/login/profile, cart → quote → order (`Idempotency-Key` enforced) → simulated payment, admin session + CSRF (including token rotation), category/product/inventory/order administration, order status chain `confirmed → processing → shipped → delivered` with `409 INVALID_TRANSITION` afterwards, image upload → public read → delete, Google `500` fail-closed with the explicit log line, and every SPA route. |
| Admin browser walkthrough | Sign-in with the seeded administrator, products/categories/inventory/orders rendering live API data, theme toggle switching and persisting across reload; no console or network errors beyond the expected guest `401` on `/api/v1/admin/auth/session`. |

## Why the admin and API share one origin

The admin uses Laravel Sanctum's first-party SPA session (HttpOnly `eshop_admin_session` cookie plus the
echoed `XSRF-TOKEN` cookie). Serving the SPA and API from the same HTTPS origin keeps the session cookie
first-party and removes cross-origin configuration risk.

ByetHost's free hosting also serves a short JavaScript browser challenge. A normal browser passes it
automatically, but a server-side proxy (for example a Cloudflare Pages worker) cannot. The working layout is
therefore:

- admin static files + Laravel front controller in `htdocs/`;
- Laravel application code in `htdocs/eshopverse_app/`;
- no Cloudflare Pages proxy in front of the API.

The admin source no longer ships a Cloudflare `_worker.js`; it was removed because the same-origin ByetHost
layout is the working deployment and the worker cannot pass the host challenge.

## File layout on the host

```text
htdocs/
├── index.php              # Laravel front controller (repo: deploy/byethost/htdocs/index.php)
├── .htaccess              # API/health routes + SPA fallback (repo: deploy/byethost/htdocs/.htaccess)
├── index.html             # admin SPA entry (generated by `npm run build`)
├── assets/                # hashed JS/CSS bundles
├── fonts/                 # self-hosted Lato TTFs + OFL license
└── eshopverse_app/        # Laravel application (vendor/, .env, storage/, bootstrap/cache/)
    └── .htaccess          # denies direct web access to the Laravel directory
```

`deploy/byethost/htdocs/index.php` auto-detects either supported layout:

- `htdocs/index.php` + `htdocs/eshopverse_app/` (current), or
- `htdocs/index.php` + sibling `eshopverse_app/`.

The root `.htaccess` rewrites `/api/*` and `/up` to `index.php`, serves real files (admin assets, fonts), and
falls back to `index.html` for React Router paths such as `/products`. It also denies direct access to
`.env` files.

## Required production settings

These values live only in `htdocs/eshopverse_app/.env` on the host (git-ignored locally). The checked-in
`backend/.env.example` documents the variables; substitute the real host, database, and key there.

| Variable | Production value / guidance |
|----------|-----------------------------|
| `APP_ENV` | `production` |
| `APP_DEBUG` | `false` |
| `APP_URL` | `https://eshopverse.byethost8.com` |
| `DB_CONNECTION` | `mysql` (the Laravel driver name; do not change it to the host name) |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` / `DB_USERNAME` / `DB_PASSWORD` | Values from VistaPanel → MySQL Databases; never commit them |
| `SESSION_DRIVER` | `database` |
| `SESSION_SECURE_COOKIE` | `true` behind HTTPS |
| `SESSION_DOMAIN` | `null` for this single-host layout |
| `SANCTUM_STATEFUL_DOMAINS` | `eshopverse.byethost8.com` (add the port only for local development) |
| `CORS_ALLOWED_ORIGINS` | `https://eshopverse.byethost8.com` |
| `GOOGLE_CLIENT_ID` | Google **Web application** OAuth client ID (Google Cloud Console → APIs & Services → Credentials). Required for `POST /auth/google`; not a secret. Empty makes the endpoint fail closed with `500` plus an explicit log entry |
| `GOOGLE_CLIENT_IDS` | Optional comma-separated additional trusted Google client IDs (for example the admin web panel or an iOS build) |
| `DEMO_ADMIN_PASSWORD` | Private demo value used when seeding; never commit it or place it in docs |
| `APP_KEY` | Generated once on the host with `php artisan key:generate` when the app was prepared; keep the same key or sessions/cookies invalidate |

### Google Sign-In client ID

`POST /api/v1/auth/google` is disabled until a Google OAuth client ID is configured. The API is the
only verifier: it checks the ID token locally against Google's published keys, so no client secret is
stored in the backend.

1. Open Google Cloud Console, select the project that owns the demo, and go to
   **APIs & Services → Credentials**.
2. Choose **Create credentials → OAuth client ID**, application type **Web application**. The Android
   app uses this Web client ID as its server client ID and ID-token audience; an Android-type or
   installed-app client ID is not accepted here (Google issues the ID token for the Web client, so its
   `aud` would not match).
3. For the web admin sign-in, add the deployed origin (for example
   `https://eshopverse.byethost8.com`) under **Authorized JavaScript origins**.
4. Copy the **Client ID** (it ends with `.apps.googleusercontent.com`) exactly, without spaces, into
   `htdocs/eshopverse_app/.env` as `GOOGLE_CLIENT_ID=...`.
5. If other first-party clients (admin web panel, iOS) also request ID tokens, list their client IDs
   comma-separated in `GOOGLE_CLIENT_IDS=...`. A token is accepted when its `aud` claim matches any
   configured client ID.
6. Save `.env` through the file manager. If the deployment caches configuration, rebuild that cache.

The Android OAuth client created for the app (package `com.averonlabs.eshopverse`, plus the debug and
release SHA-1 fingerprints registered in Google Cloud Console) is still required so Google Play services
can authenticate the app. It is **not** the token audience: keep both clients in the same project and use
the Web application client ID here and as the app's server client ID.

When `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_IDS` are both empty, the endpoint answers
`500 INTERNAL_ERROR`, writes `Google Auth failed: GOOGLE_CLIENT_ID is not configured in .env` to
`storage/logs/laravel.log`, and creates no account. That log line is the signal to fix this setting; it
never contains a token or credential.

The admin production build uses `admin/.env.production`:

```dotenv
VITE_API_BASE_URL=/api/v1
```

The relative path is intentional: the SPA and API are on the same origin. `admin/.env.example` keeps the
local `http://localhost:8000/api/v1` default for development.

## Database import

The live database was imported through phpMyAdmin from a SQL export generated from the backend migrations and
seeders. The migration files remain the schema source of truth; the seeded demo data is fictional.

For a fresh import:

1. Create the MySQL database and user in VistaPanel.
2. Open phpMyAdmin for that database.
3. Import the generated SQL export.
4. Put the same database name, user, password, and host in `eshopverse_app/.env`.
5. Verify the API returns products before opening the admin.

Do not run test migrations against the live database.

## Build and upload the admin update

From the repository root:

```powershell
Set-Location admin
npm ci
npm run typecheck
npm run lint
npm run check:contrast   # light + dark token pairs
npm test
npm run build
```

`npm run build` writes `admin/dist/`. The upload archive is created from the **contents** of `admin/dist`
so it extracts directly into `htdocs`. The current package is `output/eshopverse-admin.zip`; the earlier
`deploy/eshopverse-admin-dist.zip` name referred to the same layout and is no longer kept in the tree:

```text
output/eshopverse-admin.zip
├── index.html
├── assets/
└── fonts/
```

Upload steps:

1. In VistaPanel's file manager, back up the current `htdocs/index.html`, `htdocs/assets/`, and
   `htdocs/fonts/` if you want an easy rollback.
2. Upload `deploy/eshopverse-admin-dist.zip` into `htdocs`.
3. Extract it in `htdocs`, overwriting `index.html`, `assets/`, and `fonts/`.
4. Delete any old `htdocs/_worker.js` left from the earlier Cloudflare-oriented build; the new archive no
   longer includes it.
5. Confirm `htdocs/index.php` and `htdocs/.htaccess` are present. If they were removed, upload the copies
   from `deploy/byethost/htdocs/` (enable “Show hidden files” for `.htaccess`).
6. Do not extract `eshopverse_app` into `htdocs/public` or replace the Laravel directory; the backend is
   already deployed and unchanged.
7. Open `https://eshopverse.byethost8.com`, hard-refresh with `Ctrl+F5`, and sign in.

If the host still serves an old bundle, delete the old hashed files in `htdocs/assets/` after confirming the
new `index.html` references the new bundle.

## Verification checklist

| Check | Expected result |
|-------|-----------------|
| `GET https://eshopverse.byethost8.com/up` with `Accept: application/json` | HTTP 200 and `{"status":"up"}` (a browser navigation renders Laravel’s HTML health page) |
| `GET https://eshopverse.byethost8.com/api/v1/products` | JSON list of seeded active products |
| `GET https://eshopverse.byethost8.com/login` | Admin sign-in SPA loads; no 404 from the host |
| Sign in with the seeded administrator | Session cookie set; dashboard loads |
| Theme toggle | Switches light/dark immediately; reload keeps the saved choice; no light flash before paint |
| Browser console | No API/CORS/cookie errors |
| `GET /api/v1/admin/auth/session` after sign-out | `401 UNAUTHENTICATED` |

If the API returns `500`, inspect `htdocs/eshopverse_app/storage/logs/laravel.log` and confirm the `.env`
database values and `APP_KEY`. If assets load but routes 404, confirm `.htaccess` is present and the host
allows `mod_rewrite`.

## ByetHost notes and limitations

- **PHP version:** select PHP 8.3 or newer in VistaPanel. Laravel 13 does not run on PHP 8.2 or older.
- **No SSH:** use phpMyAdmin for the database and the file manager for uploads; there is no `artisan` shell.
- **Upload size:** free-hosting file-manager limits may require uploading `eshopverse-admin-dist.zip` rather
  than loose files. The current archive is about 1.07 MB.
- **Hidden files:** `.htaccess` is hidden by default; enable hidden-file display before renaming or uploading.
- **Writable directories:** `eshopverse_app/storage/` and `eshopverse_app/bootstrap/cache/` must be writable
  (755, or 777 only if the host requires it for the demo).
- **Browser challenge:** normal browsers pass it; server-side proxy tools do not. Keep the API same-origin.
- **Free-hosting availability:** the demo can be suspended or rate-limited by the host; this is not a
  production SLA.

## Rollback

1. Restore the previous `htdocs/index.html`, `htdocs/assets/`, and `htdocs/fonts/` backup.
2. Keep `htdocs/index.php`, `htdocs/.htaccess`, and `htdocs/eshopverse_app/` unchanged.
3. Hard-refresh the browser.

The theme update changes only the admin frontend. Rolling it back does not affect the database or API.

## Related records

- [UI-003 in the Admin Roadmap](ADMIN_ROADMAP.md) — theme task, acceptance criteria, and verification record.
- [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) — admin light/dark tokens and toggle behavior.
- [SCREEN_SPECIFICATIONS.md](SCREEN_SPECIFICATIONS.md) — admin shell and sign-in screen details.
- [HANDOFF.md](HANDOFF.md) — reviewer setup and screen walkthrough.
- [SECURITY_REVIEW.md](SECURITY_REVIEW.md) — browser-storage and non-production boundaries.
