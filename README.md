# Asahi

Standalone Android TV / Google TV / NVIDIA Shield streaming app.

## Status
Early implementation / Android runtime validation phase.

Current repo state:
- architecture/docs are in place
- feature slices for metadata, auth, sources, and playback are scaffolded
- the broader app graph is reconnected on Android startup and the app now launches on the emulator again
- the shared networking layer has been migrated away from JVM-only `java.net.http.HttpClient` to an OkHttp-backed transport in `core:network`
- `android.permission.INTERNET` is now declared so live network paths can actually run on device
- bootstrap gaps are tracked in `docs/BOOTSTRAP_GAPS.md`
- a real Gradle wrapper is present in the repo
- GitHub Actions builds a debug APK artifact successfully
- current direction remains a fully in-app provider model (no custom backend required)
- local emulator testing is now working end-to-end for install / launch / log capture
- TMDb live metadata is working in the debug preview path
- Real-Debrid device auth is now proven end-to-end: start, browser approval, poll, token exchange, and persisted token reuse all work
- Real-Debrid polling was fixed to use `device_code` correctly
- RD token persistence now lands at the expected app debug path and survives fresh JVM runs used by the preview/debug tasks
- Torrentio live source fetching works and returns real hashed results
- a debrid-aware Torrentio path is now wired using the persisted RD token, so cached RD-usable results can surface even when RD `instantAvailability` returns `disabled_endpoint`
- the live source flow now prefers real Torrentio results when enabled instead of mixing fake/sample providers into the same preview slice
- source/provider plumbing is alive and cache-aware in preview; the main next gap is moving from debug-heavy preview flows into a real TV sources/playback UI

## Current runtime/debug mode
The app is no longer in the temporary startup-safe auth-only mode.
Current behavior is:
- the normal startup path is re-enabled
- the app includes both phone (`LAUNCHER`) and TV (`LEANBACK_LAUNCHER`) launcher categories for easier sideload testing
- emulator launch has been re-verified after the networking migration
- debug preview/auth runners remain important for probing TMDb, Real-Debrid, and source pipeline behavior while the real TV UI is still minimal
- the preview auth panel now reports persisted linked-state correctly when a token file is present instead of always implying a fresh device-flow start
- the preview source panel can now show live Torrentio-only results plus RD-tagged/cached counts when Torrentio is enabled and linked state exists

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
