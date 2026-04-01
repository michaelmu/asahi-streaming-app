# Asahi Bootstrap Gaps

Current snapshot of what still blocks the repo from feeling like a real Android project bootstrap.

## Confirmed gaps
- Gradle wrapper now exists and the project builds successfully with `./gradlew assembleDebug`
- emulator install / launch / log capture is now verified locally
- no actual screen host framework yet beyond the current debug-text-driven shell
- no final UI stack choice has been fully exercised in a real TV UI host
- dependency set is still intentionally minimal/partial rather than a mature final app stack
- live TMDb and Real-Debrid flows now work, including persisted token reuse in debug/preview runs, but the app still lacks a real TV-first sources/playback UI

## Structural observations
- module/build layout is now coherent enough to continue from
- feature slices have consistent wiring patterns
- app-level coordinator exists
- app shell is visible, but still largely placeholder/debug-oriented
- the shared transport layer is now insulated behind app-local abstractions and backed by OkHttp instead of JVM-only `java.net.http.HttpClient`
- recent Android/runtime validation shifted the main risk from boot failure to incomplete feature/UI depth and end-to-end product flow polish

## Recommended next bootstrap tasks
1. keep the real Gradle wrapper committed and healthy
2. decide whether to keep the app on plain Activity temporarily or move immediately to Compose/View host
3. continue adding real Android/Kotlin/UI dependencies intentionally instead of all at once
4. keep emulator/device validation in the loop as feature flows become more real
5. shift focus from startup survival to usable TV UI flow, source selection, playback, and end-to-end slice depth
