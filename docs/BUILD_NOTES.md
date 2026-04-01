# Asahi Build Notes

Current state: the repo is still early and may not build cleanly yet, but the goal is to progressively reduce fake scaffolding.

Update: the real Gradle wrapper has now been generated successfully. The next step is verifying actual project/module build behavior.

A debug preview runner is now also being used as a lightweight way to inspect runtime provider/metadata flow without needing the full Android UI path.
Use mainstream/high-confidence test titles for provider probing so identifier/provider failures are easier to interpret.

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
- apply the same insulation rule to source feed construction, not just TMDb/client wiring
- scaffold real providers behind the provider template before binding to a concrete upstream, so the buildable baseline stays intact
- apply the same live-first, fallback-safe approach to details metadata as search becomes more real
- keep the debug preview rich enough to show whether search/details are genuinely surfacing live metadata characteristics
- surface identifier fidelity (especially IMDb vs TMDb) when source providers depend on specific IDs like Torrentio does
- once a real provider is returning results, tighten parsing and ranking before chasing provider breadth
- introduce cache-awareness into the source pipeline early, even if the first pass is placeholder-only, so ranking and preview surfaces can adapt to the right product shape
- replace placeholder cache marking with a real instant-availability-style path as soon as the pipeline shape is proven
- make preview playback use the actual top-ranked source so debug output reflects the real source-selection behavior
- evolve the sources side toward explicit provider adapters before swapping in real transports/providers
- transitional transport-shaped adapters are still useful, but the target architecture is now fully in-app provider logic rather than dependence on remote addon endpoints
- decide when to switch from placeholder text UI to real Android TV UI host
- avoid adding more feature complexity until bootstrap gaps are reduced
