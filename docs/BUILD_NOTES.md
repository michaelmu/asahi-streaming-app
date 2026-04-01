# Asahi Build Notes

Current state: the repo is still early and may not build cleanly yet, but the goal is to progressively reduce fake scaffolding.

Update: the real Gradle wrapper has now been generated successfully. The next step is verifying actual project/module build behavior.

A debug preview runner is now also being used as a lightweight way to inspect runtime provider/metadata flow without needing the full Android UI path.
Use mainstream/high-confidence test titles for provider probing so identifier/provider failures are easier to interpret.
The debug preview should also expose Real-Debrid device-auth state so auth and source behavior can be inspected in the same runtime surface.
When auth is not behaving, include enough debug signal (API mode, bootstrap client id, raw response/error) to diagnose the device/code start path before adding more auth complexity.
For human-in-the-loop device auth, use persisted local debug state across CLI runs rather than in-memory state, since each Gradle invocation starts a fresh JVM.
Use a dedicated polling task for device auth so the app can wait and retry instead of assuming immediate credential availability.
For the most reliable manual testing, prefer a single live auth-flow runner that starts and polls in the same process.
The in-app auth debug surface should follow the same rule: start and poll inside one running session rather than relying on disconnected manual steps.

Android runtime update: local emulator testing now works for install/launch/log capture. That testing exposed a real Android incompatibility in the current networking layer: the app graph still reaches `java.net.http.HttpClient`, which is JVM-only and crashes on Android with `NoClassDefFoundError`. Until the HTTP stack is migrated to an Android-compatible client, keep startup isolated from the broader app graph so the APK remains launchable for runtime validation.

## Immediate priorities
- keep Android app resources/manifest sane
- keep module conventions centralized
- avoid adding more fake UI than necessary
- prefer a minimal debuggable activity over invisible placeholder app code
- make bootstrap expectations explicit even before the real wrapper/build verification exists

## Near-term next steps
- replace the current JVM-only HTTP layer with an Android-compatible client (preferably OkHttp)
- keep module/plugin setup internally consistent
- align Android/JVM target settings when Gradle surfaces toolchain mismatches
- avoid enabling Compose in the app convention until the app actually carries Compose runtime dependencies and a real Compose UI host
- use the debug shell to expose small end-to-end preview paths for real slices while the full TV UI is still pending
- reintroduce live RD auth only after the startup path no longer touches JVM-only networking classes
- introduce real network integrations behind existing repository/api/mapper boundaries, with graceful fallback to fake adapters while the buildable baseline is protected
- when real adapters are introduced, prefer minimal JSON parsing first over premature full DTO systems, so the live path can be verified quickly
- keep app-layer wiring insulated from transport/client implementation types; expose factories or repository boundaries instead
- use the debug shell to show whether live integrations appear to be returning real data versus fallback placeholders
- apply the same insulation rule to source feed construction, not just TMDb/client wiring
- scaffold real providers behind the provider template before binding to a concrete upstream, so the buildable baseline stays intact
- apply the same live-first, fallback-safe approach to details metadata as search becomes more real
- keep the debug preview rich enough to show whether search/details are genuinely surfacing live metadata characteristics
- surface identifier fidelity (especially IMDb vs TMDb) when source providers depend on specific IDs like Torrentio does
- once a real provider is returning results, tighten parsing and ranking before chasing provider breadth
- introduce cache-awareness into the source pipeline early, even if the first pass is placeholder-only, so ranking and preview surfaces can adapt to the right product shape
- replace placeholder cache marking with a real instant-availability-style path as soon as the pipeline shape is proven
- keep the Real-Debrid HTTP path config-gated so the preview and build remain usable even before credentials/tokens are fully configured
- keep app-layer DI insulated from debrid transport/client construction just like TMDb/source feed wiring
- evolve the sources side toward explicit provider adapters before swapping in real transports/providers
- transitional transport-shaped adapters are still useful, but the target architecture is now fully in-app provider logic rather than dependence on remote addon endpoints
- decide when to switch from placeholder text UI to real Android TV UI host
- avoid adding more feature complexity until the Android runtime path is clean
