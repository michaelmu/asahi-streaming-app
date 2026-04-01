# Asahi

Standalone Android TV / Google TV / NVIDIA Shield streaming app.

## Status
Initial architecture and scaffolding phase.

Current repo state:
- architecture/docs are in place
- feature slices for metadata, auth, sources, and playback are scaffolded
- app-level coordinator/navigation spine exists
- temporary UI shell placeholder exists at the app layer
- the app activity now renders a minimal debug text view instead of doing nothing
- bootstrap gaps are tracked in `docs/BOOTSTRAP_GAPS.md`
- a real Gradle wrapper is now present in the repo
- current direction: fully in-app provider model (no custom backend required)

## Planning docs
See `docs/` for the current planning set:
- `docs/RESEARCH.md`
- `docs/ARCHITECTURE_PLAN.md`
- `docs/DOMAIN_CONTRACTS.md`
- `docs/SCAFFOLDING_PLAN.md`
- `docs/IMPLEMENTATION_ROADMAP.md`
