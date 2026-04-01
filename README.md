# Asahi

Standalone Android TV / Google TV / NVIDIA Shield streaming app.

## Status
Initial architecture and scaffolding phase.

Current repo state:
- architecture/docs are in place
- feature slices for metadata, auth, sources, and playback are scaffolded
- app-level coordinator/navigation spine exists
- temporary UI shell placeholder exists at the app layer
- the app activity currently defaults to an RD auth debug/test surface
- bootstrap gaps are tracked in `docs/BOOTSTRAP_GAPS.md`
- a real Gradle wrapper is now present in the repo
- current direction: fully in-app provider model (no custom backend required)

## Current auth test mode
The app is currently set up as an RD auth test build:
- launches directly into the auth debug screen
- starts device auth immediately
- polls in-session for longer
- shows final linked/auth-in-progress/error state clearly

## Planning docs
See `docs/` for the current planning set:
- `docs/RESEARCH.md`
- `docs/ARCHITECTURE_PLAN.md`
- `docs/DOMAIN_CONTRACTS.md`
- `docs/SCAFFOLDING_PLAN.md`
- `docs/IMPLEMENTATION_ROADMAP.md`
