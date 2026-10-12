# Android Development Instructions

For GitHub Copilot and other IDE-integrated agents working on Android code.

## References

- Primary instructions: `/AGENTS.md`
- Android-specific: `/android/AGENTS.md`
- Android roadmap and canonical task steps: `/docs/ANDROID_ROADMAP.md`
- Android task brief format, reading sets, and file ownership: `/docs/android/TASK-BRIEF-TEMPLATE.md`
- Android phase implementation specs: `/docs/android/PHASE-0-FOUNDATION-SPEC.md`, `/docs/android/PHASE-1-SPEC.md`, `/docs/android/PHASE-2-SPEC.md`
- Machine-readable API contract: `/docs/openapi.yaml`; contract mock strategy: `/docs/android/API-MOCK-STRATEGY.md`
- Project phase gates and shared tasks: `/docs/ROADMAP.md`
- Architecture: `/docs/ARCHITECTURE.md`
- API Contract: `/docs/API_CONTRACT.md`
- Design System: `/docs/DESIGN_SYSTEM.md`
- Screen specifications: `/docs/SCREEN_SPECIFICATIONS.md`
- Testing: `/docs/TESTING.md`

## Key Rules

- Language: Java
- UI: XML layouts with Material 3
- Architecture: MVVM + Repository Pattern
- `strings.xml` is shell-owned; feature screens add their own `values/strings_<feature>.xml` (rule from `android/AGENTS.md`)
- Follow naming conventions in `android/AGENTS.md`
- API communication only through `docs/openapi.yaml` (accepted 2026-10-12 — follow it exactly; make contract changes there first)
- Read only the reading set your task brief names; do not read the whole `docs/` tree
- Never edit a file owned by another task (shell, Gradle, manifest, theme/colour/dimension/style tokens, or another feature's package)
- Never parse money into a `double`; use the shared fixed-precision money type and formatter
- No new colours, dimensions, styles, or typography in a screen; report a missing token instead
