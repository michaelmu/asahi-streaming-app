# Asahi Build Notes

Current state: the repo is still early and may not build cleanly yet, but the goal is to progressively reduce fake scaffolding.

Update: the real Gradle wrapper has now been generated successfully. The next step is verifying actual project/module build behavior.

## Immediate priorities
- keep Android app resources/manifest sane
- keep module conventions centralized
- avoid adding more fake UI than necessary
- prefer a minimal debuggable activity over invisible placeholder app code
- make bootstrap expectations explicit even before the real wrapper/build verification exists

## Near-term next steps
- add Gradle wrapper / verify build bootstrap
- make module/plugin setup internally consistent
- fix buildSrc/root plugin resolution and versioning issues surfaced by first bootstrap attempts
- fix first compile-time dependency gaps exposed by real builds (currently coroutine/Flow dependencies in domain and playback integration)
- align Android/JVM target settings when Gradle surfaces toolchain mismatches
- avoid enabling Compose in the app convention until the app actually carries Compose runtime dependencies and a real Compose UI host
- use the debug shell to expose small end-to-end preview paths for real slices while the full TV UI is still pending
- prefer enriching those preview paths across search/details/sources/playback before investing heavily in placeholder UI chrome
- introduce real network integrations behind existing repository/api/mapper boundaries, with graceful fallback to fake adapters while the buildable baseline is protected
- when real adapters are introduced, prefer minimal JSON parsing first over premature full DTO systems, so the live path can be verified quickly
- keep app-layer wiring insulated from transport/client implementation types; expose factories or repository boundaries instead
- use the debug shell to show whether live integrations appear to be returning real data versus fallback placeholders
- decide when to switch from placeholder text UI to real Android TV UI host
- avoid adding more feature complexity until bootstrap gaps are reduced
