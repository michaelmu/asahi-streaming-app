# Asahi

Standalone Android TV / Google TV / NVIDIA Shield streaming app.

## Status
Active implementation with real Android runtime validation, signed release builds, and a validated multi-provider source stack.

Current repo state:
- Android app launches on emulator and TV-oriented flows are actively being exercised
- shared networking is now Android-safe via the OkHttp-backed `core:network` layer
- Real-Debrid device auth works end-to-end, including persisted token reuse
- in-app update flow exists, including signed release APK support and update gating by higher remote build number
- GitHub Actions now builds and publishes a **signed release APK**
- reusable modal/overlay UI exists for auth, updates, and user-facing status/error flows
- app icon / TV banner assets are now wired into the app manifest
- source/provider architecture has been cleaned up for multiple real providers
- validated provider stack currently includes:
  - Torrentio
  - Comet
  - BitSearch
  - Knaben
- provider visibility in the UI has been improved with clearer provenance and diagnostics

## Current product direction
The current direction remains a fully in-app provider model with no required custom backend.

Practical focus areas now are:
- improving the browse → source selection → playback experience on TV
- continuing to thin the app shell by extracting workflow coordinators out of `MainActivity`
- improving provider visibility, diagnostics, and eventual toggles
- keeping the provider stack high-signal instead of porting a large number of brittle scrapers
- continuing real-device/emulator validation alongside fast JVM sanity checks

## CI / APK artifacts
GitHub Actions is configured to build and publish a **signed release APK** from `main`.

Current artifact/release direction:
- rolling signed release build published as `asahi-release.apk`
- release signing is wired through GitHub Secrets
- local release signing is also supported for manual builds

See:
- `docs/RELEASE_SIGNING_SETUP.md`

## Operational docs
Useful docs for day-to-day work:

- `docs/RELEASE_SIGNING_SETUP.md`
- `docs/EMULATOR_TESTING.md`
- `docs/PROVIDER_RESEARCH.md`
- `docs/PROVIDER_ARCHITECTURE_NOTES.md`
- `docs/UPGRADE_FLOW_NOTES.md`
- `docs/APP_SHELL_REVIEW_2026-04-04.md`

## Planning / reference docs
See `docs/` for broader planning and architecture notes, including:
- `docs/RESEARCH.md`
- `docs/ARCHITECTURE_PLAN.md`
- `docs/DOMAIN_CONTRACTS.md`
- `docs/SCAFFOLDING_PLAN.md`
- `docs/IMPLEMENTATION_ROADMAP.md`
