# Asahi

Standalone Android TV / Google TV / NVIDIA Shield streaming app.

## Status
Early implementation / Android runtime validation phase.

Current repo state:
- architecture/docs are in place
- feature slices for metadata, auth, sources, and playback are scaffolded
- app-level coordinator/navigation spine exists, but app startup is currently isolated from the broader graph for Android runtime safety
- the app activity currently defaults to a standalone auth debug/test surface
- bootstrap gaps are tracked in `docs/BOOTSTRAP_GAPS.md`
- a real Gradle wrapper is present in the repo
- GitHub Actions builds a debug APK artifact successfully
- current direction remains a fully in-app provider model (no custom backend required)
- local emulator testing is now working end-to-end for install / launch / log capture

## Current auth test mode
The app is currently set up as an RD auth test build:
- launches directly into a standalone auth debug screen
- includes both phone (`LAUNCHER`) and TV (`LEANBACK_LAUNCHER`) launcher categories for easier sideload testing
- currently avoids initializing the broader app graph on startup so it can run on Android while JVM-only networking code is still being replaced
- is intentionally in a startup-safe mode after emulator testing showed the broader graph was crashing on Android due to `java.net.http.HttpClient`

## CI / APK artifacts
GitHub Actions is configured to build a debug APK on pushes to `main` and via manual workflow dispatch.
The workflow uploads the APK as a downloadable artifact named `asahi-debug-apk`.

## Planning docs
See `docs/` for the current planning set:
- `docs/RESEARCH.md`
- `docs/ARCHITECTURE_PLAN.md`
- `docs/DOMAIN_CONTRACTS.md`
- `docs/SCAFFOLDING_PLAN.md`
- `docs/IMPLEMENTATION_ROADMAP.md`
