# Asahi Bootstrap Gaps

Current snapshot of what still blocks the repo from feeling like a real Android project bootstrap.

## Confirmed gaps
- Gradle wrapper now exists and can be generated successfully in this repo
- no verified app/module build run has happened yet beyond wrapper/bootstrap success
- no Android resource/theme structure beyond the minimal string file
- no actual screen host framework yet (only debug text rendering)
- no real dependency set for Android/Compose/Media3/Hilt/etc.
- no local/network implementation readiness for TMDb or Real-Debrid

## Structural observations
- module/build layout is now coherent enough to continue from
- feature slices have consistent wiring patterns
- app-level coordinator exists
- app shell is visible, but placeholder only
- first real bootstrap attempt exposed `buildSrc` and root plugin-resolution/versioning issues, which is useful because it identifies the next concrete cleanup target

## Recommended next bootstrap tasks
1. keep the real Gradle wrapper committed and healthy
2. decide whether to keep the app on plain Activity temporarily or move immediately to Compose/View host
3. add real Android and Kotlin dependencies intentionally
4. run real build/bootstrap verification
5. fix compile/resource/config issues as they surface before adding more product logic
