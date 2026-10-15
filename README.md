# EShop Verse

A portfolio e-commerce application with an Android customer app, REST API, and a small admin dashboard. It demonstrates a complete English-language shopping flow; it is not a live commerce service.

---

## Overview

EShop Verse is a single-store portfolio project for an international audience. The Android customer app and admin dashboard use the same REST API. The first release uses English UI and sample USD prices, and checkout uses a simulated payment flow.

## Features

### Customer Application (Android)
- Product browsing, categories, search, and product details
- Account registration and sign-in
- Shopping cart and checkout with simulated payment
- Order history and status

### Admin Dashboard
- Product, category, and inventory management
- Order status management

### Backend API
- RESTful API serving both client applications
- User authentication and authorization
- Product catalog management
- Order processing
- Simulated checkout and payment outcomes for demonstrations

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐
│   Android App   │     │ Admin Dashboard  │
│   (Customer)    │     │     (Web)        │
└────────┬────────┘     └────────┬─────────┘
         │                       │
         └───────────┬───────────┘
                     │
              ┌──────▼──────┐
              │  REST API   │
              │   (PHP)     │
              └──────┬──────┘
                     │
              ┌──────▼──────┐
              │   MySQL     │
              │  Database   │
              └─────────────┘
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture documentation.

---

## Technology Stack

| Component | Technologies |
|-----------|-------------|
| Android App | Java, XML, Material 3, MVVM, Repository Pattern, Retrofit (Room deferred per ADR-004) |
| Backend | PHP, Laravel, RESTful API |
| Admin Dashboard | React, TypeScript |
| Database | MySQL |
| CI/CD | GitHub Actions |

---

## Repository Structure

```
├── android/           # Android customer application
├── backend/           # PHP REST API
├── admin/             # Admin dashboard
├── docs/              # Project documentation
│   ├── decisions/     # Architecture Decision Records
│   └── *.md           # Topic-specific documentation
├── .ai/               # AI agent workspace
├── .github/           # GitHub configuration
│   ├── ISSUE_TEMPLATE/
│   ├── workflows/
│   └── instructions/
├── AGENTS.md          # AI agent instructions (provider-neutral)
├── CONTRIBUTING.md    # Contribution guidelines
└── LICENSE            # MIT License
```

---

## Development Workflow

This project uses a branch-based workflow:

1. Fetch the latest remote refs and create a task branch from `develop`.
2. Implement changes with focused commits.
3. Open a Pull Request.
4. Pass CI checks and code review.
5. Merge task PRs to `develop`; promote a completed release to `main` only through a reviewed release PR.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for full details.

---

## AI-Assisted Development

This repository is designed for collaborative development between human developers and AI coding agents. The multi-agent workflow is documented in:

- [AGENTS.md](AGENTS.md) — Provider-neutral agent instructions
- [Build](.agents/workflows/build.md), [independent test](.agents/workflows/test.md), and [conditional debug](.agents/workflows/debug.md) workflows
- [.ai/README.md](.ai/README.md) — AI workspace documentation

Supported agents include Claude Code, OpenAI Codex, Google Gemini/Antigravity, GitHub Copilot, and other compatible tools.

---

## Documentation

| Document | Description |
|----------|-------------|
| [PRODUCT.md](docs/PRODUCT.md) | Product requirements and vision |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture |
| [API_CONTRACT.md](docs/API_CONTRACT.md) | API conventions and contracts |
| [DATABASE.md](docs/DATABASE.md) | Database design |
| [DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) | UI/UX design system |
| [SCREEN_SPECIFICATIONS.md](docs/SCREEN_SPECIFICATIONS.md) | Page-by-page Android and admin dashboard layouts and states |
| [SECURITY.md](docs/SECURITY.md) | Security guidelines |
| [TESTING.md](docs/TESTING.md) | Testing strategy |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Development workflow |
| [DECISIONS.md](docs/DECISIONS.md) | Architecture decisions index |
| [ROADMAP.md](docs/ROADMAP.md) | Cross-component tasks and final project/release validation |
| [ANDROID_ROADMAP.md](docs/ANDROID_ROADMAP.md) | Sole Android execution roadmap: phases, numbered steps, status, and canonical task criteria |
| [BACKEND_ROADMAP.md](docs/BACKEND_ROADMAP.md) | Backend phases, numbered steps, and canonical task criteria |
| [ADMIN_ROADMAP.md](docs/ADMIN_ROADMAP.md) | Admin phases, numbered steps, and canonical task criteria |

---

## Project Status

**Phase 0 — Foundation**

The first-release product scope and engineering workflow are documented. Feature completion has not yet been verified.

**Backend API: ✅ complete and verified (2026-10-15).** All Backend Roadmap Phases 0–3 are delivered — customer auth, catalog, cart, checkout/orders with idempotency, simulated payment, and the admin session/catalog/inventory/order APIs — with 153 passing tests (2062 assertions), clean style and dependency audits. See [docs/BACKEND_ROADMAP.md](docs/BACKEND_ROADMAP.md), [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md), and [docs/HANDOFF.md](docs/HANDOFF.md).

Use the relevant component roadmap to execute and report component work. Use [docs/ROADMAP.md](docs/ROADMAP.md) for cross-component tasks and final project/release validation only.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

The Android app bundles the Lato Regular, Semibold, and Bold font files from the [Google Fonts Lato family](https://github.com/google/fonts/tree/main/ofl/lato), distributed under the SIL Open Font License 1.1. The license notice is included at [android/licenses/Lato-OFL.txt](android/licenses/Lato-OFL.txt).

Copyright (c) 2026 Averon Labs
