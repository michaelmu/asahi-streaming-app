# Asahi TV UX Borrowed Patterns Pass

Last updated: 2026-04-05 UTC
Status: IN_PROGRESS
Owner: OpenClaw main session
Location: `docs/exec-plans/in_progress/2026-04-05-tv-ux-borrowed-patterns-pass.md`
Supersedes: none
Superseded by: 

## Purpose

This plan turns the Red Wizard / Kodi Estuary APK review into a scoped implementation pass for `asahi-streaming-app`.

It is a UX/product-to-implementation plan focused on borrowing the *useful TV patterns* without copying Kodi’s heavy complexity or plugin-driven behavior.

The core problem it is solving:
- the app already has strong search/results/source selection foundations, but it still risks feeling like a tool rather than a polished TV destination
- the APK review showed a few proven remote-first interaction patterns worth adopting: dynamic home shelves, richer focus states, better empty states, and lighter-weight playback/status overlays
- these patterns need to be translated into Asahi’s actual codebase and priorities rather than copied literally

This pass is intentionally incremental. It should improve first-screen usefulness and browse/playback confidence without forcing a full information architecture rewrite.

It also includes a visual-language sub-pass: not to copy Red Wizard or Kodi branding, but to identify which *classes of assets* are worth recreating in Asahi’s own style (focus treatments, panel shadows, background textures, state badges, and lightweight overlay chrome).

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not only after the work.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes when direction shifts
- validation notes after completed steps

### Completion rule
A task is only `DONE` when:
- the code landed
- relevant tests/build/manual validation happened
- follow-up work is captured here if needed

---

## Status Legend

- `TODO` = not started
- `IN_PROGRESS` = actively being worked
- `BLOCKED` = waiting on a prerequisite or decision
- `DONE` = implemented and validated
- `DEFERRED` = intentionally postponed
- `OPTIONAL` = not required unless chosen

---

## Current Focus

**Current phase:** Phase B/C transition — shipped browse/home UX, deciding remaining polish

**Immediate target:** keep the plan honest about what has already landed locally, what is still being validated on-device, and whether any playback/status or visual-kit work still belongs in this pass

**Why this now:**
The pass is no longer at the “translate inspiration into work” stage. The main browse/home slices have already landed locally, and the useful planning work now is to track those changes precisely, keep the remaining scope tight, and avoid letting optional polish work drift into an open-ended redesign.

> Update this section whenever the active phase or immediate target changes.

---

## Repository Reality Check

Before implementation begins, confirm:
- the repo already contains prior execution-plan work for favorites, watch history, activity extraction, ranking, and UI polish
- the current canonical plan set lives under `docs/exec-plans/`
- `2026-04-04-next-pass-ranking-orchestration.md` is already in `complete/`, so this UX plan is a fresh pass rather than a continuation of that document
- actual browse, search, favorites, and playback entry points were re-read in the current branch during A1 and are currently orchestrated centrally from `app/src/main/kotlin/ai/shieldtv/app/MainActivity.kt` via `renderCurrentScreen()`
- current navigation/state transitions are driven by `app/src/main/kotlin/ai/shieldtv/app/AppCoordinator.kt` and `app/src/main/kotlin/ai/shieldtv/app/AppState.kt`; favorites/history browse are not separate destinations, but `RESULTS` variants keyed by `favoritesBrowseMode` / `historyBrowseMode`
- current home, search, results, details, episodes, sources, player, and settings UI renderers are all defined together in `app/src/main/kotlin/ai/shieldtv/app/ui/ScreenRenderers.kt`, not split into per-file renderer classes
- current home already contains more dynamic behavior than the earlier plan implied: `HomeScreenRenderer.render(...)` surfaces Continue Watching, Quick Picks, Favorites, Watch History, Browse actions, and Recent Searches using existing local/stateful data plus a small baked-in featured fallback set
- current search results are rendered in `app/src/main/kotlin/ai/shieldtv/app/ui/ScreenRenderers.kt` by `ResultsScreenRenderer.render(...)`, which today builds a vertical list of horizontal media cards rather than a poster wall/grid
- the current results surface is capped to `state.searchResults.take(20)` and re-used for plain search, favorites browse, and watch-history browse, so any poster-wall/grid change likely needs to preserve those three modes or consciously split them
- current player rendering is also centralized in `ScreenRenderers.kt` via `PlayerScreenRenderer.render(...)`; the app already uses Media3 controller UI plus a custom app overlay that currently only appears on playback error / `playbackState.errorMessage != null`, so the “lighter overlay” work should start by clarifying that condition rather than assuming a larger overlay stack already exists
- current modal/overlay affordances outside playback are handled by `app/src/main/kotlin/ai/shieldtv/app/ui/OverlayPopup.kt`, which already provides focus-trapped panels and may be reusable for some empty-state or action-sheet polish but is not a playback HUD
- the APK review source was a Kodi-derived app package, so the borrowable material is interaction design, screen behavior, and generic visual pattern categories rather than Android-native implementation details or app branding
- likely useful visual source categories were confirmed in the extracted APK: background patterns (`skin.estuary/extras/backgrounds/*`), panel/dialog chrome (`themes/*/dialogs/*`, `themes/*/overlays/shadow.png`), state overlays (`OverlayWatched.png`, `OverlayUnwatched.png`, `OverlayWatching.png`), focus/list treatments (`list_focus.png`, panel/shadow assets), and media-tech badge systems (`media/flagging/*`)
- direct-branded assets also exist (`assets/media/banner.png`, `vendor_logo.png`, splash assets), and those should be treated as explicit non-borrow targets

---

## Locked Decisions

- Borrow interaction patterns and generic visual patterns, not branding, bundled content, addon behavior, or permission strategy.
- Keep the app TV-first and remote-first; every accepted idea must work well with D-pad navigation.
- Prefer a small number of high-leverage UX upgrades over a skin-system explosion.
- Home should become more action-oriented and recommendation-oriented, but not at the cost of making search harder to reach.
- Search remains a first-class entry point, and search results should evolve toward a poster-wall/grid presentation if the remote focus behavior can stay strong.
- Rich focus states should reveal useful metadata already available locally; do not create fragile mandatory fetches on focus.
- Empty states must be actionable, not decorative.
- Playback/status overlays should become more lightweight and progressive, but this pass should not destabilize core playback.
- Avoid importing Kodi-style “every view for every situation” complexity unless real app usage justifies it.
- Recreate visual assets in Asahi’s own style rather than shipping Red Wizard-branded or Kodi-derived art blindly.
- Favor a small reusable design kit over a large dump of one-off image assets.

---

## Background / Review Summary

The APK inspection showed that most of Red Wizard’s useful UX comes from Kodi Estuary conventions:
- home shelves that answer “what now?” quickly
- multiple browse view types optimized for different tasks
- cards that reveal richer context on focus instead of requiring full drill-in
- persistent progress/watched state directly on cards
- purposeful empty states with clear next actions
- lightweight loading and playback overlays that preserve context

The parts *not* worth borrowing are equally important:
- installer-style first-run automation
- heavy permission-first startup
- plugin complexity and sprawling skin condition trees
- broad customization surface area before the core UX is stable

For Asahi, the opportunity is to adapt these ideas into a simpler product shape:
- stronger home rows/shelves fed by existing favorites/history/search data
- improved browse/result cards with better focus detail and state chips
- search results that feel like a TV browse surface instead of a plain stacked list, ideally through a poster-wall/grid treatment with clear focus behavior
- better empty-state copy and action buttons
- a cleaner playback overlay/status model
- a cleaner visual kit built around background textures, focus rings/plates, card shadows, watched/progress badges, and simple quality/codec chips

The asset review showed a useful distinction:
- **worth borrowing conceptually or recreating:** soft background patterns, translucent dialog/panel surfaces, shadow/elevation assets, watched/in-progress/favorite state overlays, list/card focus treatments, and codec/resolution badge taxonomy
- **not worth borrowing directly:** Red Wizard splash/logo/banner/vendor art, QR assets, or wholesale Kodi skin dumps used unchanged

This plan assumes the app should keep its current product identity: fast, opinionated streaming-focused, not a general-purpose media center.

---

# Phase A — UX Translation and App Mapping

## A1. Audit current screen structure against the borrowed patterns
Status: DONE
Priority: High

### Goal
Identify the real app screens, coordinators, adapters, and models that would own home shelves, result focus states, browse views, and playback overlays.

### Why this matters
The repo has already proven that assuming the wrong file shapes wastes time. This pass should begin by mapping the real UI structure before proposing implementation details.

### Proposed sub-steps
- [DONE] Re-read the current Android TV entry flow and main navigation structure.
- [DONE] Identify the real browse/results/favorites/history surfaces and their adapters/view holders/composables.
- [DONE] Identify the real playback controls or overlay entry points.
- [DONE] Record mismatches between the ideal UX slices and the current app architecture.

### A1 findings
- Main screen orchestration still lives in `MainActivity.renderCurrentScreen()`, which switches shells, wires callbacks, and invokes renderer classes directly.
- The app is still classic Android Views with handwritten `LinearLayout`/`FrameLayout` composition via `ScreenViewFactory`; there are no RecyclerView adapters or Compose screens to plug into for a grid rewrite.
- Home is already closer to a dynamic dashboard than the plan initially implied. It is not a blank slate: it already uses continue-watching, favorites, watch history, recent searches, and browse actions, plus quick-pick fallbacks.
- Favorites and history are currently implemented as flavored `RESULTS` screens rather than separate browse destinations. That makes shared card primitives attractive, but it also means a results-grid rewrite can easily spill into favorites/history behavior if not scoped carefully.
- Results today are rendered as a simple vertical stack of custom horizontal cards, with focus behavior implemented per-card inside `ResultsScreenRenderer` rather than by a reusable browse/list infrastructure.
- Player UI is mostly Media3 controller UI plus a very small custom overlay path in `PlayerScreenRenderer`; there is not yet a richer internal overlay/state-layer system waiting to be styled.
- Because renderers are grouped in one large `ScreenRenderers.kt` file, the cheapest execution path is likely targeted renderer extraction or helper extraction around the touched surfaces, not a broad UI architecture rewrite as part of this UX pass.

### Validation
- Notes added to this plan’s `Repository Reality Check` and `Progress Log`.
- File/class targets are named concretely before any implementation task is marked `IN_PROGRESS`.

---

## A2. Define the minimum viable “borrowed patterns” set for this pass
Status: DONE
Priority: High

### Goal
Reduce the inspiration set into a small, shippable batch of UX improvements.

### Why this matters
Without an explicit cut line, this will sprawl into a full home/browse/player redesign.

### Proposed sub-steps
- [DONE] Choose the top 3-5 UX upgrades with the best leverage.
- [DONE] Mark which ideas are explicitly deferred.
- [DONE] Translate each chosen idea into concrete user-visible outcomes.

### Accepted scope for this pass
- Convert results/favorites/history browsing to a poster-grid presentation that still behaves well with D-pad navigation.
- Enrich focused poster states using locally available metadata only.
- Improve empty states so favorites/history/search/home feel actionable rather than dead-end.
- Tighten the home surface into a more shelf-like landing experience using existing local/stateful data.
- Improve fresh-search focus/query behavior so the screen behaves like a clean search entry point instead of inheriting stale browse state.

### Explicit deferrals / non-goals
- No full information-architecture rewrite.
- No Kodi-style multiple view-mode explosion.
- No recommendation engine or speculative network shelves just to make home look busy.
- No branding/theme cloning from Red Wizard or Kodi.
- No risky playback-engine rewrite just to support overlay polish.

### Validation
- A final accepted scope is written into this plan.
- Deferred/non-goals are explicit and honest.

---

# Phase B — Home and Browse Improvements

## B1. Add or improve home shelves that answer “what should I do now?”
Status: DONE
Priority: High

### Goal
Create a more useful landing experience with dynamic, high-intent rows such as continue watching, favorites, recent discoveries, or best available picks.

### Why this matters
The APK’s strongest UX pattern is a home screen that gives immediate choices. Asahi currently benefits from stronger first-screen usefulness if users can reach content without always starting at search.

### Proposed sub-steps
- [DONE] Define an MVP shelf set capped at 2-4 high-intent content rows, with browse/search utilities retained below them.
- [DONE] Restrict first-pass shelves to rows powered by existing local/stateful data now.
- [DONE] Add shelf-style home wiring using the current renderer structure rather than introducing a new home architecture.
- [DONE] Ensure rows are ordered by user usefulness, not implementation convenience.
- [DONE] Handle empty shelf cases cleanly.
- [DONE] Explicitly defer recommendation-engine behavior or shelves that require speculative network fetches just to look populated.

### Implemented shape
- Home now favors a shelf-style flow built from current data sources: Continue Watching, Quick Picks, combined Your Picks, combined Recently Watched, then Browse / Recent Searches utility sections.
- The change intentionally tightens the landing surface rather than pretending to be a full recommendation system.

### Validation
- Manual navigation confirms shelves are reachable and sensible with the remote.
- Tests cover any new state/model mapping logic where practical.
- No shelf depends on brittle network fetches merely to render a placeholder state.
- The shipped home surface remains intentionally small rather than pretending to be a full recommendation system.

---

## B2. Convert search results into a poster wall / TV grid
Status: DONE
Priority: High

### Goal
Change search results from a stacked vertical list into a poster-wall/grid presentation that feels native to Android TV browsing.

### Why this matters
The current results renderer uses horizontally arranged list cards. That works functionally, but it undersells browsing and makes the results screen feel more like a utility screen than a destination. A poster wall better matches the product direction and the APK-inspired TV browsing patterns.

### Proposed sub-steps
- [DONE] Re-read `ResultsScreenRenderer` and the surrounding screen composition to confirm the cheapest path to a grid/panel layout.
- [DONE] Decide whether favorites/history should share the same poster-wall renderer or keep a distinct presentation.
- [DONE] Implement a poster-first card layout with remote-friendly spacing, aspect ratio, and conservative row/column density suitable for TV distance.
- [DONE] Preserve click and long-press actions while making D-pad traversal predictable.
- [DONE] Ensure the grid still supports empty-state and footer actions like "New Search" without awkward focus traps.
- [DONE] Define poster fallback behavior for missing artwork so sparse metadata does not collapse the layout.

### Implemented shape
- Search, favorites browse, and watch-history browse now share a poster-grid direction through the existing `RESULTS` flow rather than splitting into separate destination types.
- The results surface was converted from stacked horizontal cards into a TV-oriented multi-column poster grid with fallback artwork handling.

### Validation
- Manual navigation verifies smooth left/right/up/down traversal across the full result set.
- Posters remain legible and do not clip awkwardly at common TV resolutions.
- Title and secondary metadata remain readable at TV distance.
- Selection/long-press behavior still works correctly from the new grid.
- Focus is retained sensibly when results update, repopulate, or paginate.
- Favorites/history rendering is validated if they share the same renderer.

---

## B3. Enrich focus states on browse/result cards
Status: DONE
Priority: High

### Goal
Make focused cards reveal more value immediately: synopsis, year/runtime, source count, best quality, watch progress, favorite state, or similar metadata already available in-app.

### Why this matters
TV UIs benefit when focus acts like preview. This reduces unnecessary click depth and makes browsing feel more alive.

### Proposed sub-steps
- [DONE] Identify which metadata is already available for focused items without expensive extra fetches.
- [DONE] Lock a preferred metadata priority order for the first pass around locally available cues.
- [DONE] Design a focus treatment that stays legible at TV distance.
- [DONE] Implement richer focused/unfocused states in the relevant result/browse views.
- [DONE] Avoid layout jumps that make D-pad browsing feel unstable.
- [DONE] Make the focus treatment work well specifically in the new poster-wall search results layout.

### Implemented shape
- Focused poster cards now reveal richer locally available cues such as year/media-type/favorite/watched context, stronger focus styling, and clearer action hints.
- The implementation intentionally avoids focus-triggered network fetches or heavy synopsis expansion that would make navigation feel sluggish.

### Validation
- Manual browse testing verifies focus movement remains smooth and predictable.
- New focus treatment does not cause clipping/overlap in common content cases.
- Focused metadata remains readable without overwhelming the card.
- Existing tests are updated or expanded where state mapping changes.

---

## B4. Add actionable empty states across key media surfaces
Status: DONE
Priority: Medium

### Goal
Replace dead-end empty screens with context-aware prompts and actions.

### Why this matters
Empty states are a major part of perceived polish, especially on TV surfaces where users can feel “stuck” quickly.

### Proposed sub-steps
- [DONE] Inventory current empty states for favorites, history, search results, and home shelves.
- [DONE] Replace generic/no-content copy with explicit next actions.
- [DONE] Ensure actions are remote-accessible and lead somewhere useful.

### Validation
- Manual checks cover each targeted empty state.
- Copy and actions are consistent with actual navigation destinations.

---

# Phase C — Playback and Status Polish

## C0. Define an Asahi visual asset kit inspired by the APK review
Status: TODO
Priority: Medium

### Goal
Turn the extracted visual inspiration into a small, app-native asset kit that supports the rest of this pass.

### Why this matters
The UX changes in Phases B and C will land better if they share a coherent visual language. The APK contains useful categories of presentation assets, but they should mostly be recreated rather than copied as-is.

### Proposed sub-steps
- [TODO] Inventory the visual patterns to keep: soft background texture(s), focus ring/plate, card shadow, dialog surface, watched/in-progress/favorite badges, and quality/codec chips.
- [TODO] Mark exact asset categories to avoid using directly: Red Wizard logos, splash art, banners, QR/promotional assets, and any obviously branded repository art.
- [TODO] Decide which assets should be image-based, vector-drawn, or implemented via Compose/View styling instead of raw files.
- [TODO] Define a minimal first-wave asset pack that supports home shelves, focused cards, and playback overlays.

### Validation
- The plan records a concrete borrow/recreate/avoid list before implementation starts.
- The first-wave asset kit is small enough to stay maintainable and broad enough to support the accepted UI changes.

---

## C0.1. Turn the asset review into a borrow / recreate / avoid inventory with exact Asahi targets
Status: TODO
Priority: High

### Goal
Convert the broad visual asset guidance into a concrete inventory of exact target assets for Asahi, grouped by what to borrow conceptually, what to recreate directly, and what to avoid.

### Why this matters
Right now the asset guidance is directionally useful but still abstract. This task makes it implementation-ready by naming the exact Asahi-side assets/components that should exist.

### Proposed sub-steps
- [TODO] Create a borrow / recreate / avoid table or bullet inventory inside this plan or a linked asset note.
- [TODO] Name the exact first-wave Asahi targets, such as: poster focus ring, poster shadow plate, search-result card background, empty-state panel surface, playback top-strip background, watched/in-progress/favorite badge set, and quality/codec chip set.
- [TODO] For each target, note whether it should be drawable asset, vector asset, or code-rendered styling.
- [TODO] Map each target asset to the screens/components expected to use it first.
- [TODO] Record any assets that are intentionally deferred to avoid overbuilding the design kit.

### Validation
- The inventory names exact target assets/components for Asahi rather than only describing inspiration categories.
- The inventory is specific enough that implementation can start without redoing the visual-analysis pass.
- Deferred assets are documented explicitly.

---

## C1. Introduce lighter-weight playback/status overlays
Status: TODO
Priority: Medium

### Goal
Improve playback feedback with a layered, less disruptive status overlay before full controls are shown.

### Why this matters
The APK review highlighted a good pattern: users often need quick reassurance (progress, title, pause/seek state) before they need a full control surface.

### Proposed sub-steps
- [TODO] Identify current playback overlay/control architecture in the real repo.
- [TODO] Separate lightweight playback status from heavier control/detail surfaces if feasible.
- [TODO] Surface minimal progress/time/metadata cleanly during pause/seek transitions.
- [TODO] Keep this pass scoped away from risky playback-engine rewrites.
- [TODO] If the work begins to require deeper player-engine or timing-sensitive state changes, stop and split the overlay work into a follow-up plan instead of forcing it into this pass.

### Validation
- Manual playback testing covers play, pause, seek, and resume transitions.
- Overlay timing/focus behavior is stable under remote input.
- Known limitations are documented if full validation is not possible.
- Any split/follow-up decision is logged explicitly rather than implied.

---

## C2. Improve loading/busy states without losing context
Status: TODO
Priority: Medium

### Goal
Make loading feel intentional and less jarring by preserving the surrounding context when possible.

### Why this matters
Abrupt blocking states make TV apps feel clunky. Small loading indicators, dim overlays, or inline busy states can preserve flow.

### Proposed sub-steps
- [TODO] Audit current loading patterns in search, source loading, browse, and playback preparation.
- [TODO] Replace the most disruptive full-screen/blocking states where low-risk.
- [TODO] Keep loading affordances visible enough to communicate progress.

### Validation
- Manual flow checks confirm users can still understand that work is happening.
- Regressions like double-spinners or trapped focus do not appear.

---

# Optional Work

## O1. Add a browse view toggle for list vs poster/detail-heavy layouts
Status: OPTIONAL
Priority: Low

### Notes
This is attractive in theory because Kodi benefits from multiple views, but it may be overkill for the current app unless the existing browse surface is already structurally close. Only pull this in if Phase B reveals a cheap, honest implementation path.

---

## O2. Add shelf personalization/ranking rules
Status: OPTIONAL
Priority: Low

### Notes
Once home shelves exist, they can become smarter. But this should wait until the baseline rows are useful and predictable.

---

## Recommended Order

1. A1. Audit current screen structure against the borrowed patterns
2. A2. Define the minimum viable “borrowed patterns” set for this pass
3. B2. Convert search results into a poster wall / TV grid
4. B3. Enrich focus states on browse/result cards
5. B4. Add actionable empty states across key media surfaces
6. B1. Add or improve home shelves that answer “what should I do now?”
7. Validate fresh-search entry/focus behavior and clean up any state bugs exposed by the browse changes
8. Decide whether C0/C0.1 visual-kit work is still worth doing in this pass
9. Only then consider C1 playback/status overlays or C2 loading-state polish if they remain localized and low-risk

---

## Open Questions / Decisions Needed

### Q1. Should this pass introduce a true new home screen, or improve the existing entry surface incrementally?
Current recommendation:
Prefer incremental improvement. A1 confirmed the current home screen already behaves like a lightweight dashboard, so this pass should improve and reorder that surface rather than replace it wholesale unless a specific blocker appears.

### Q2. Which metadata is safe to reveal on focus without causing fetch churn?
Current recommendation:
Only use locally available or already-fetched metadata in this pass. If extra data is desirable, cache/populate it through existing flows rather than focus-triggered requests.

### Q3. Should playback overlay work be included in the same pass as home/browse work?
Current recommendation:
Only if the overlay architecture is localized and low-risk. A1 suggests the custom playback overlay layer is currently thin and tightly coupled to `PlayerScreenRenderer`, so home/browse work should probably land first and playback polish should split out quickly if it starts demanding deeper controller-state changes.

### Q4. Are favorites/history mature enough to drive home recommendations now?
Current recommendation:
Likely yes for a first pass, but only for a deliberately small home MVP. Confirm the current state and edge cases before locking shelf definitions, and avoid pretending this is a full recommendation system.

### Q5. Which visual assets should be recreated as original Asahi assets versus implemented through code/styling?
Current recommendation:
Recreate only the pieces that materially improve TV readability and polish: focus treatments, shadows, simple textured backgrounds, state badges, and quality chips. Prefer code/styling for plain surfaces and progress treatments when possible.

### Q6. Is it worth directly reusing any extracted non-branded asset files?
Current recommendation:
Treat direct reuse as a licensing/design review item rather than the default path. For now, assume “recreate, don’t copy” unless a specific asset is confirmed safe, generic, and worth the dependency.

### Q7. Should favorites and watch history use the same poster-wall renderer as normal search results?
Current recommendation:
Probably yes at the primitive level because all three flows already share `RESULTS`, but not necessarily as identical full-screen composition. A1 confirmed that favorites/history are mode-flavored results screens today, so the safest path is likely a shared poster card/grid primitive with room for browse-specific labels and actions.

### Q8. Where should the exact borrow / recreate / avoid inventory live?
Current recommendation:
Start inside this exec plan unless it becomes too bulky; split into a linked design/asset note only if the inventory grows enough to hurt plan readability.

### Q9. Should the progress log track exact commits for completed milestones in this pass?
Current recommendation:
Yes. This pass is already half historical and half forward-looking, so meaningful landed slices should name the commit hash alongside validation notes. Do not log every tiny tweak; track milestone commits that materially changed the plan state.

---

## Risks / Watchouts

- Planning against outdated UI structure again instead of the real branch.
- Taking on a full IA redesign under the banner of “borrowed ideas.”
- Adding focus richness that causes visual instability or sluggishness during D-pad navigation.
- Converting search results to a grid in a way that breaks expected focus order, footer actions, or long-press affordances.
- Creating shelves that look smart but are empty/noisy because the underlying data is too thin.
- Letting playback polish creep into risky player-state rewrites.
- Copying Kodi patterns that feel heavy or over-configured in a simpler streaming app.
- Accidentally letting visual inspiration turn into asset theft, brand confusion, or a mismatched Kodi-like identity.
- Creating too many image assets when code-drawn or theme-driven UI would be cleaner.

---

## Validation Notes / Honesty Check

### Plan creation
- Validated by: APK structure review, current exec-plan directory check, canonical plan inventory, and visual asset category inventory from the extracted package.
- Not validated at creation time: actual current app UI file/class targets for home, browse, and playback; licensing suitability of any direct asset reuse.
- Known uncertainty at creation time: the initial draft intentionally stopped short of naming exact implementation files until the repo mapping pass was done, and it assumed recreate-over-copy for most visual assets.

### Mid-pass implementation state
- Validated by: local renderer/code inspection, repeated `testDebugUnitTest` and `assembleDebug` runs during shipped slices, and emulator/manual checks for home/search/results behavior.
- Not fully validated yet: whether any playback/status overlay follow-up is worth the risk in this same pass; full real-device validation of every poster-grid edge case; a completed asset-kit inventory.
- Known uncertainty: emulator Android TV IME behavior still makes automated results-entry capture a little misleading unless the keyboard is explicitly dismissed/submitted, so screenshot evidence should be interpreted with that caveat.

---

## Progress Log

### 2026-04-05 17:34 UTC
- Created the plan from the Red Wizard / Kodi APK UX review.
- Captured the main borrowable patterns: home shelves, rich focus states, actionable empty states, lighter overlays, and less disruptive loading states.
- Explicitly marked non-goals: branding/theme cloning, plugin behavior, first-run installer behavior, and Kodi-level view complexity.
- No implementation work completed yet.
- Commit: `bbdad7c` (`Refine TV UX borrowed patterns exec plan`)

### 2026-04-05 17:37 UTC
- Expanded the plan to include a visual asset strategy derived from the APK inspection.
- Recorded the main asset categories worth recreating: background textures, panel/dialog chrome, focus treatments, watched/in-progress state overlays, and quality/codec badge systems.
- Explicitly excluded direct reuse of Red Wizard-branded splash, banner, vendor, and promotional assets.
- Added a dedicated asset-kit task so the visual layer is planned alongside UX behavior rather than bolted on later.

### 2026-04-05 17:39 UTC
- Re-checked the current app search-results implementation before changing the plan.
- Confirmed `ResultsScreenRenderer` currently renders search results as a vertical stack of horizontal cards, not a poster wall.
- Added a dedicated high-priority task to convert search results into a TV-friendly poster wall/grid and wired follow-up focus-state work to that new layout.

### 2026-04-05 17:40 UTC
- Added a dedicated planning task to turn the asset guidance into a concrete borrow / recreate / avoid inventory with exact Asahi targets.
- Explicitly called out first-wave asset targets like poster focus treatment, shadow plate, result-card surface, empty-state panel, playback strip surface, state badges, and quality/codec chips.
- Kept the recommendation lightweight: start the inventory inside this plan unless it grows too large.

### 2026-04-05 17:46 UTC
- Completed A1 by re-reading the current app flow in `MainActivity`, `AppCoordinator`, `AppState`, `ScreenRenderers.kt`, and `OverlayPopup.kt`.
- Confirmed the app already has a dashboard-style home surface backed by continue-watching, favorites, watch history, recent searches, browse actions, and quick-pick fallback content.
- Confirmed search, favorites, and history all currently converge on the same `RESULTS` destination and `ResultsScreenRenderer`, which today renders a vertical stack of horizontal cards capped to 20 items.
- Confirmed there is no separate grid/list adapter infrastructure yet; renderer work is handwritten view composition inside `ScreenRenderers.kt`.
- Confirmed playback overlays are currently thin: Media3 controller UI plus a small app-side overlay path in `PlayerScreenRenderer`, so Phase C should stay cautious.
- Updated the plan to recommend incremental home improvement, shared result-card primitives, and likely prioritizing browse/result work before trying to redesign playback UI.
- Commit: `a2d2a9d` (`Ground TV UX plan in current app structure`)

### 2026-04-05 18:xx UTC
- Completed the main browse/home UX implementation slices this plan was created to drive.
- Search, favorites, and watch history now route through a poster-grid-style results presentation rather than the old stacked horizontal-card list.
- Focus states were enriched using locally available metadata and stronger TV-distance affordances instead of fetch-on-focus behavior.
- Empty states across home/results/favorites/history were made more actionable.
- Home was tightened into a shelf-style landing surface built from existing data rather than speculative recommendation fetches.
- Commit: `de23f07` (`Convert results screen to poster grid`)
- Commit: `1eac112` (`Enrich poster grid focus states`)
- Commit: `a9c5bef` (`Improve actionable empty states`)
- Commit: `25eb184` (`Tighten home screen into shelf-style layout`)
- Validation: repeated `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` runs passed during these slices; emulator/manual checks confirmed the new home/search/results flows were rendering and navigable.

### 2026-04-05 18:xx UTC
- Continued the pass with search-entry cleanup prompted by emulator/manual validation.
- Improved fresh-search focus flow so search opens on a saner input target.
- Found and fixed a real state bug where a fresh search could inherit stale `Favorites` / `Watch History` browse labels because browse-mode reset did not also clear the active query.
- This was a meaningful UX correctness fix, not just emulator cleanup.
- Commit: `df6219b` (`Improve search screen focus flow`)
- Commit: `73845de` (`Clear browse labels from fresh search state`)
- Validation: emulator/manual checks reached a correct empty Movies search surface with the input focused and no bogus inherited query text.

---

## Scope Changes

### 2026-04-05
- Initial scope established.
- Future hooks to preserve: shelf model flexibility, locally available metadata for focus enrichment, a small reusable asset kit, a reusable poster-card primitive for search/favorites/history, and a possible later split of playback overlay polish into its own plan if Phase C proves riskier than expected.
- Scope has since effectively narrowed around honest midpoint tracking: the main browse/home changes are now landed locally, while visual-kit and playback/loading polish remain explicitly optional until proven worth the risk.

---

## Session Start

### 2026-04-05 17:46 UTC
Intended task: execute A1 by mapping the real repo structure for home, results/favorites/history, and playback overlays, then update the plan with concrete targets and constraints.

### 2026-04-05 18:40 UTC
Intended task: update the plan so it cleanly tracks the already-landed local UX work by milestone commit, while keeping the remaining future scope explicit and narrow.

---

## Definition of Done

This plan is complete for its intended pass when:
- the accepted borrowed-pattern items are implemented, validated, and logged
- deferred ideas are marked honestly rather than left implied
- home/browse/playback changes (if included) are mapped to real app structure and landed cleanly
- validation notes explain what was actually exercised manually or by tests
- `Current Focus` no longer implies unfinished required work
- the file is ready to move out of `in_progress/` without misleading anyone
