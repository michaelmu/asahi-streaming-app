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

Android runtime update: local emulator testing now works for install/launch/log capture. That testing exposed a real Android incompatibility in the networking layer: `java.net.http.HttpClient` was crashing Android with `NoClassDefFoundError`. That transport path has now been replaced with an OkHttp-backed client in `core:network`, the broader startup path has been reconnected, and emulator launch succeeds again.

Follow-up runtime finding: once the transport crash was fixed, the next Android failure was a missing `INTERNET` permission. That has now also been fixed in the manifest, so live network code can run on-device.

Current validation snapshot:
- app installs and launches on the emulator with the broader graph enabled
- TMDb live search/details work in the debug preview path
- Real-Debrid device auth is proven end-to-end in manual testing: start, browser approval, poll, and token exchange all succeed
- the RD polling path was fixed to use `device_code` instead of `user_code`
- debug token storage now persists across fresh JVM runs for validation purposes
- source pipeline preview runs without the old transport/runtime crash
- current source-provider limitations are now about provider config/wiring breadth rather than Android-incompatible HTTP code

## Immediate priorities
- keep Android app resources/manifest sane
- keep module conventions centralized
- avoid adding more fake UI than necessary
- prefer a minimal debuggable activity over invisible placeholder app code
- make bootstrap expectations explicit even before the real wrapper/build verification exists

## Near-term next steps
- keep module/plugin setup internally consistent
- align Android/JVM target settings when Gradle surfaces toolchain mismatches
- avoid enabling Compose in the app convention until the app actually carries Compose runtime dependencies and a real Compose UI host
- use the debug shell to expose small end-to-end preview paths for real slices while the full TV UI is still pending
- continue introducing real network integrations behind existing repository/api/mapper boundaries, with graceful fallback to fake adapters while the buildable baseline is protected
- when real adapters are introduced, prefer minimal JSON parsing first over premature full DTO systems, so the live path can be verified quickly
- keep app-layer wiring insulated from transport/client implementation types; expose factories or repository boundaries instead
- use the debug shell to show whether live integrations appear to be returning real data versus fallback placeholders
- stop conflating "preview panel starts a new auth flow" with "actual persisted RD linked state"; those are separate concerns now
- apply the same insulation rule to source feed construction, not just TMDb/client wiring
- scaffold real providers behind the provider template before binding to a concrete upstream, so the buildable baseline stays intact
- apply the same live-first, fallback-safe approach to details metadata as search becomes more real
- keep the debug preview rich enough to show whether search/details are genuinely surfacing live metadata characteristics
- surface identifier fidelity (especially IMDb vs TMDb) when source providers depend on specific IDs like Torrentio does
- explicitly enable and validate Torrentio/live source providers rather than assuming provider presence implies active results
- once a real provider is returning results, tighten parsing and ranking before chasing provider breadth
- introduce cache-awareness into the source pipeline early, even if the first pass is placeholder-only, so ranking and preview surfaces can adapt to the right product shape
- replace placeholder cache marking with a real instant-availability-style path as soon as the pipeline shape is proven
- keep the Real-Debrid HTTP path config-gated so the preview and build remain usable even before credentials/tokens are fully configured
- keep app-layer DI insulated from debrid transport/client construction just like TMDb/source feed wiring
- evolve the sources side toward explicit provider adapters before swapping in real transports/providers
- transitional transport-shaped adapters are still useful, but the target architecture is now fully in-app provider logic rather than dependence on remote addon endpoints
- decide when to switch from placeholder text UI to real Android TV UI host
- now that the Android runtime path is clean enough to launch, focus on deeper feature-flow validation rather than more startup isolation work
