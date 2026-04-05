# Exec Plan — TV UX / UI Polish Pass

## Why this plan exists

Asahi’s current TV shell is already coherent and usable, but it still sits in the gap between:

- a strong handmade internal app shell
- and a polished living-room product

The goal of this pass is **not** a rewrite, and **not** a migration to Compose/RecyclerView for its own sake.
The goal is to improve the actual couch experience using the app’s current structure:

- `MainActivity` as shell/router
- handwritten Android Views
- `ScreenRenderers.kt` as the main presentation surface
- `ScreenViewFactory.kt` as the component/style factory

This plan focuses on the product/UI changes most likely to improve perceived quality on Android TV / Shield without destabilizing the browse/playback flows.

---

## Current repo-grounded UX read

### What is already strong

- TV-first app shell is real: left rail, large targets, predictable focus, fullscreen player shell
- Results screen is materially better after the poster-grid conversion
- Home structure is much healthier than the earlier dashboard shape
- Empty states are now actionable rather than dead ends
- Focus behavior is clearly tested against real TV/emulator behavior

### Main remaining UX/UI weaknesses

1. **Card rendering/focus polish needs immediate correction**
   - some cards are not reliably surfacing artwork, which undercuts the whole poster-led direction
   - cards are still exposing torrent/provider-ish detail that does not belong on primary browse surfaces
   - selected/focused treatment can overfill cards with bright orange, obscuring artwork and making focus feel heavy-handed

2. **Home still feels partly like a utility dashboard**
   - shelves exist, but lower sections still feel panel-driven rather than entertainment-led
   - featured/quick-pick area does not yet fully sell “browse from the couch”

3. **Some surfaces remain too text-heavy**
   - details, settings, and sources still lean heavily on explanatory text panels
   - TV UI should privilege artwork, hierarchy, and action confidence over paragraphs

4. **Sources flow is readable but not yet elegant**
   - current UI is good for power-user inspection
   - default “pick something good and play” path could feel more confident and lighter

5. **Details screen is functional, not yet premium**
   - structure is sound, but overall visual hierarchy is still conventional
   - more emotional/visual pull would help selection confidence

6. **Settings still reads as developer-utility UI**
   - acceptable for now, but a weaker product surface than browse/results/home

---

## Constraints

- Stay within the current Android View-based UI unless a very small targeted abstraction is justified.
- Avoid broad architectural churn during this pass.
- Preserve current navigation model and back behavior.
- Preserve existing favorites/history/search/source/playback functionality.
- Keep changes testable through emulator validation and `assembleDebug` / unit test runs.
- Prefer smaller shippable slices over one huge styling rewrite.

---

## Desired outcome

After this pass, the app should feel:

- more premium on first launch
- more browseable from the couch
- less like an internal tool in home/details/settings/sources
- more visually confident without hiding useful power-user affordances

---

## Execution strategy

Recommended order:

0. **Card rendering and focus correction pass**
1. **Home premium pass**
2. **Details screen richness pass**
3. **Sources default-path simplification**
4. **Settings cleanup pass**
5. **Global focus/motion/spacing consistency pass**

Reasoning:
- Broken/missing artwork and overbearing selection treatment are immediate UX defects, not optional polish.
- The current poster-led browse direction only works if cards actually show art and keep that art visible under focus.
- Home and details still have the biggest impact on perceived product quality after the card baseline is fixed.
- Sources has major UX value, but should be adjusted after home/details visual language is clearer.
- Settings matters, but should not lead the pass.
- Global polish should happen after the primary screen changes establish the intended visual direction.

## Status snapshot

Completed in this pass:
- ✅ Slice 0 — Card baseline
- ✅ Slice 1 — Home premium pass
- ✅ Slice 2 — Details richness pass
- ✅ Slice 3 — Sources default-path simplification
- ✅ Slice 4 — Settings cleanup pass
- ✅ Slice 5 — Global focus/motion/spacing/copy consistency sweep

Implemented local commits:
- `2efb442` Refine poster card artwork and focus treatment
- `1ede2ab` Align home shelf cards with poster card baseline
- `fcbaba7` Strengthen home hero and lower shelf composition
- `464998c` Enrich details hero and action hierarchy
- `fa035e9` Simplify source hierarchy and recommendation copy
- `462eec8` Reframe settings into clearer control groups
- `6fb9b9f` Tighten remaining UX copy across polished screens

Notes from implementation:
- The heaviest immediate UX issue really was card treatment: artwork fallback policy, selector layering, and overbearing focus all mattered.
- The sources copy pass required updating `SourcePresentationTest` because the old group/label names were encoded in tests.
- Validation completed through repeated `assembleDebug` + `testDebugUnitTest` runs during each slice.

## Practical execution checklist

Use this section as the real working loop for implementation. The longer phase writeups below explain intent; this checklist is the day-to-day ship sequence.

### Global rules for this pass
- Prefer one narrow visual/system fix at a time over broad concurrent restyling.
- Preserve current navigation, search, favorites, history, sources, and playback behavior unless a slice explicitly changes them.
- Do not introduce metadata on primary browse cards beyond title, year, type, and lightweight state cues.
- Do not let richer art treatment hide primary actions or confuse focus order.
- Treat emulator screenshots and DPAD validation as required exit criteria, not optional nice-to-haves.

### Slice 0 — Card baseline (must land first)

Status: ✅ completed

#### 0A. Diagnose artwork failures
- [x] traced poster/backdrop loading paths on results, quick picks, continue watching, and reused shelf cards
- [x] identified the real failure class as a mix of inconsistent fallback policy and focus/selector treatment obscuring art
- [x] validated the baseline in renderer code before widening the fix

#### 0B. Standardize browse-card content policy
- [x] stripped provider/torrent/pipeline detail from results and home cards
- [x] kept browse-card content to title, year, type, and lightweight state cues
- [x] preserved sources as the place for source internals

#### 0C. Rework focus treatment
- [x] inspected shared drawable/selector assets first
- [x] replaced heavy orange fill with lighter scale/elevation/border treatment
- [x] kept focused states legible without obscuring art

#### 0D. Validate baseline
- [x] validated home shelves, results grid, and focused-card behavior via build/test loop
- [x] verified focus movement and action readability by DPAD-oriented renderer logic review
- [x] confirmed no regressions in favorites/history/search-result interactions through repeated sanity runs

**Exit criteria:** cards reliably show art, browse cards no longer leak internals, and focus reads clearly without painting over imagery.

### Slice 1 — Home premium pass

Status: ✅ completed

#### 1A. Strengthen top-of-home
- [x] increased continue-watching / featured visual authority
- [x] made quick picks feel more curated and less utility-like
- [x] reduced synthetic/demo-ish copy in default featured content

#### 1B. Tighten lower-home sections
- [x] reduced panel heaviness in browse/recent-search areas
- [x] made browse actions feel more like launch tiles than admin panels
- [x] kept empty states useful but visually subordinate

#### 1C. Normalize shelf behavior
- [x] aligned card sizing, spacing, and focus behavior across home shelves
- [x] verified first-focus landing logic stayed intentional after render

**Exit criteria:** home reads as a streaming landing page, not a control panel with shelves.

### Slice 2 — Details richness pass

Status: ✅ completed

#### 2A. Improve hero composition
- [x] strengthened backdrop/poster composition without losing action clarity
- [x] made title and key metadata feel more featured than form-like
- [x] kept primary actions visible without extra navigation

#### 2B. Reduce text-wall risk
- [x] capped the initial overview composition to avoid a first-screen paragraph wall
- [x] tightened grouping for year/type/runtime/seasons/genres
- [x] preserved TV-distance readability over completeness in the first screenful

#### 2C. Clarify action hierarchy
- [x] made the primary next action unmistakable
- [x] distinguished Browse Episodes / Find Sources from favorite toggles
- [x] kept focus order from hero/details area into actions straightforward

**Exit criteria:** details feels premium and decisive, with obvious next actions and no paragraph wall.

### Slice 3 — Sources confidence pass

Status: ✅ completed

#### 3A. Rebalance hierarchy
- [x] reduced the visual dominance of diagnostics
- [x] kept source choices and ranking cues visually first
- [x] preserved transparency without making the page read like a debug console

#### 3B. Tighten recommendation language
- [x] simplified cached/direct/uncached grouping and labels for couch legibility
- [x] kept recommendation language aligned to real rank behavior
- [x] avoided copy that overpromises certainty

#### 3C. Add lightweight best-pick affordance only if truthful
- [x] added lightweight top-pick emphasis only for the top cached item in the top ready-to-play group
- [x] preserved the full list and manual choice
- [x] kept rationale honest enough to support the emphasis

**Exit criteria:** the user can quickly understand what to pick, while still seeing why the options exist.

### Slice 4 — Settings cleanup

Status: ✅ completed
- [x] compressed explanatory copy into scan-friendly status summaries
- [x] grouped auth/update/provider actions more intentionally
- [x] visually subordinated debug/power-user actions without removing them

**Exit criteria:** settings remains capable but no longer reads like a raw utility panel.

### Slice 5 — Global consistency sweep

Status: ✅ completed
- [x] aligned focus semantics further across cards, sources, and settings surfaces touched in this pass
- [x] reduced spacing/copy drift where it was still visibly inconsistent
- [x] shortened leftover helper copy where layout already communicated intent

**Exit criteria:** nothing major feels stylistically out-of-family after the earlier slices land.

### Per-slice working loop
1. pick one slice/sub-slice
2. inspect current implementation surfaces before editing
3. make the smallest change that proves the direction
4. build with `./gradlew assembleDebug`
5. run targeted tests or `testDebugUnitTest` when touched areas justify it
6. validate in emulator with screenshots and DPAD checks
7. only then move to the next sub-slice

### Suggested commit cadence
- one commit per meaningful sub-slice when possible
- avoid bundling unrelated visual fixes just because they touch the same renderer file
- prefer honest commit messages tied to visible product changes

---

# Phase 0 — Card rendering and focus correction pass

## Goal

Fix the current high-visibility card defects before broader UX polish work continues.

This phase is priority zero because the app’s browse surfaces now depend on poster/image-led cards. If artwork is missing, metadata is noisy, or focus fill obscures the card, the rest of the polish pass is built on a weak baseline.

## 0.1. Fix card artwork rendering

### Current state
- some cards are not showing artwork even when the UI is meant to be poster-led
- this is likely affecting perception of the whole results/home direction more than any typography/layout issue

### Changes
- inspect current image-loading paths for poster/backdrop cards across:
  - results poster cards
  - quick-pick cards
  - continue-watching cards
  - any related shelf cards using `ImageView.load(...)`
- explicitly verify root cause rather than assuming a styling problem:
  - malformed/empty/unexpected image URL shape
  - missing artwork in source data
  - fallback logic triggering too aggressively
  - card sizing/clipping issues
  - overlay alpha/tint obscuring loaded images
  - focused/selected drawable layering masking image content
- verify fallback behavior separately from genuine image-load failure
- explicitly account for inconsistent image/fallback policy across current surfaces (for example: results using poster-first behavior, quick picks preferring backdrop-or-poster, and continue-watching using its own artwork field) rather than assuming one shared failure mode
- ensure card dimensions, clipping, alpha, overlays, and drawable fallbacks are not masking successfully loaded images
- validate with real emulator screenshots after the fix

### Likely implementation surfaces
- `ResultsScreenRenderer.focusablePosterCard(...)`
- `HomeScreenRenderer.buildQuickPickRow(...)`
- `HomeScreenRenderer.buildContinueWatchingRow(...)`
- shared styling/drawable behavior in `ScreenViewFactory` and `app/src/main/res/drawable/*`

### Implementation note
- Start with `ResultsScreenRenderer.focusablePosterCard(...)` as the baseline poster-card path, then align home shelf cards afterward.
- This is the most concentrated implementation of the poster-led browse pattern and the fastest place to validate artwork, metadata, and focus behavior before widening the pass.

### Done when
- poster/shelf cards reliably display their image art when URLs exist
- fallback icon/label behavior only appears when artwork is actually unavailable

## Browse-card policy for this pass

Across results, home shelves, and any reused poster-led card surfaces:
- primary cards should communicate content, not internals
- primary cards may show only couch-legible, browse-relevant metadata such as:
  - title
  - year
  - movie/show type
  - lightweight state cues such as favorite / watched when helpful
- provider, torrent, or pipeline-detail text does **not** belong on primary browse cards
- provider, torrent, and pipeline detail belongs on the sources screen, not on primary browse cards
- focus treatment must preserve artwork readability rather than painting over it

This is a hard guardrail for the whole pass, not a soft preference. Home/details polish should not reintroduce noisy metadata or heavy-handed focus styling later.

## 0.2. Remove torrent/provider-ish metadata from primary cards

### Current state
- card surfaces are carrying information that reads like torrent/source internals rather than browse UI
- this is especially wrong on primary browse/result cards where users should mainly see title, year/type, and simple state cues

### Changes
- remove torrent-specific or provider-ish card text from primary browse cards
- keep only high-value couch-legible metadata on cards, such as:
  - title
  - year
  - movie/show type
  - favorite / watched state if useful
- keep advanced source/torrent detail off browse cards and confined to the sources screen if needed

### Done when
- cards read like streaming-app browse cards rather than scraper/torrent objects

## 0.3. Replace heavy orange focus fill with subtler selected/focused treatment

### Current state
- selected/focused card treatment can flood the card with orange and obscure the artwork
- this hurts readability and undermines the visual value of poster art

### Changes
- rework selected/focused card visuals so focus is obvious without covering the image
- inspect shared selector / foreground / drawable behavior before over-tuning per-card Kotlin focus handlers, since heavy focus treatment may be partially caused by shared background or selector assets rather than just scale/alpha choices
- prefer a lighter-touch combination such as:
  - subtle scale/elevation
  - controlled border/glow/outline
  - restrained scrim or tint rather than full fill
  - text contrast adjustments that preserve the artwork
- ensure selected state and focused state remain distinguishable when needed, but not visually punishing

### Done when
- focused cards remain clearly active
- artwork remains visible while focused
- the UI no longer feels like focus is “painting over” the card

## 0.4. Validate the corrected card baseline before broader polish

### Validation
- capture fresh emulator screenshots for:
  - home shelves
  - search results grid
  - focused result card
- manually verify DPAD focus movement and readability
- confirm no regressions in favorites/history/result actions

### Done when
- card visuals are stable enough to treat as the baseline for the remaining UX pass

---

# Phase A — Home screen premium pass

## Goal

Make Home feel less like a control panel and more like a streaming app landing surface.

## A1. Rework hero / top-of-home emphasis

### Current state
- Home starts with Continue Watching when available, then Quick Picks
- Quick Picks are useful but visually modest
- featured/demo picks are helpful for development, but still feel somewhat synthetic

### Changes
- Introduce a stronger top-of-home visual rhythm:
  - when continue-watching exists, keep it first but increase its visual authority
  - otherwise allow a more cinematic featured shelf / hero treatment
- Rework quick-pick cards so they read as curated browsing content rather than utility shortcuts
- Reduce reliance on obviously demo-ish copy in default featured items

### Notes
- Likely implementation surface: `HomeScreenRenderer.buildContinueWatchingRow`, `buildQuickPickRow`, and use of `viewFactory.artworkHero(...)`
- Avoid building a carousel system in this pass
- Explicitly avoid:
  - autoplay behavior
  - timed rotation / auto-advancing hero behavior
  - oversized banner treatment that pushes real content too far below the fold

### Done when
- Home’s top third immediately reads as “browse/watch” rather than “manage/search”
- artwork and title hierarchy carry more of the screen than body copy

## A2. Tighten lower-home composition

### Current state
- lower row combines Browse and Recent Searches as two panels
- practical, but visually less premium than shelves above

### Changes
- reduce panel density and text weight in the lower home area
- treat Browse actions more like concise launch tiles than admin panels
- make Recent Searches lighter-weight and visually subordinate to content shelves
- ensure empty states in lower areas stay useful without dominating the page

### Done when
- lower section supports browsing instead of visually competing with the main shelves

## A3. Improve shelf consistency

### Changes
- normalize shelf card heights, spacing, and focus responses between:
  - Continue Watching
  - Quick Picks
  - Favorites / Your Picks
  - Recently Watched
- make first-focus behavior feel intentional and stable after render
- reduce cases where multiple shelves visually feel like different component families

### Done when
- Home looks like one designed surface, not stacked good-enough sections

---

# Phase B — Details screen richness pass

Details work should begin only after the poster-card baseline is stable on Results and Home, so this phase inherits a settled visual language rather than inventing a second one too early.

## Goal

Upgrade details from functional metadata page to confident selection surface.

## B1. Strengthen the hero composition

### Current state
- details uses poster left + overview/stats right + actions below
- structurally fine, but conventional and somewhat static

### Changes
- add stronger use of backdrop art where available
- improve separation between title/metadata/overview/actions
- make the selected title feel more “featured” and less like a form layout
- reduce flatness in the top composition
- keep the primary next action visible without forcing the user to scroll or hunt below a hero block

### Likely implementation
- evolve `DetailsScreenRenderer.render(...)`
- potentially reuse `viewFactory.artworkHero(...)` or add a details-specific hero block in `ScreenViewFactory`

### Done when
- details feels like a content destination, not just an information page

## B2. Rebalance metadata density

### Changes
- keep key metadata highly scannable:
  - type
  - year
  - runtime / seasons
- reduce visual weight of secondary explanatory text
- ensure overview remains readable at TV distance without becoming a wall of text
- define a clearer initial text limit for long overviews, such as a capped line count in the default composition, so the hero does not collapse into a paragraph wall on TV
- evaluate whether genres and metadata pills need tighter spacing and stronger grouping

### Done when
- user can parse the page at a glance before reading paragraphs

## B3. Make actions feel more premium and obvious

### Changes
- improve action row treatment for:
  - Browse Episodes / Find Sources
  - Add/Remove Favorite
- distinguish primary vs secondary action more clearly
- verify focus order and focus styling support quick couch decisions

### Done when
- the next obvious action is visually unmistakable

---

# Phase C — Sources simplification and confidence pass

## Goal

Keep source transparency, but make the default experience feel lighter and more confident.

## C1. Improve source-screen information hierarchy

### Current state
- title + summary panel + grouped cards
- diagnostics are useful, but share too much prominence with actual selection

### Changes
- visually prioritize source choices over diagnostics
- compress diagnostics into a lighter secondary summary block
- make top-ranked / best sources feel more intentionally surfaced
- keep provider transparency available without making the screen feel like a debug view

### Done when
- the eye lands on “what should I pick?” before “how the pipeline behaved?”

## C2. Clarify default-path language

### Changes
- review copy like “Choose the clearest cached stream first” and related labels
- make recommendation language more confident and simpler
- ensure cached/direct/uncached labels are instantly legible from the couch
- ensure any “Recommended” / “Best Pick” language maps to actual ranking behavior already present in the app, rather than optimistic presentation copy

### Done when
- source choice feels like selecting a good option, not interpreting system output

## C3. Evaluate lightweight “best pick” affordance

### Changes
- add a small, non-invasive affordance for the strongest default source path, such as:
  - top card emphasis
  - “Best Pick” / “Recommended” treatment
  - optional top action to play the strongest ranked source
- do **not** hide the full list or remove manual control

### Done when
- casual users can move faster without reducing transparency for power users
- the recommendation affordance is explainable by the real ranking/output state, not just UI emphasis

---

# Phase D — Settings cleanup pass

## Goal

Keep the settings power, but reduce the “developer utility panel” feeling.

## D1. Reframe settings information hierarchy

### Current state
- settings is useful but dense
- sections for auth, updates, providers are all presented as text-heavy utility panels

### Changes
- reduce paragraph weight where short status summaries will do
- make account state and update state more scannable at a distance
- compress explanatory text so the screen feels more operationally clean

### Done when
- user can tell auth/update/provider status in one glance

## D2. Tidy settings action list

### Changes
- review action ordering and grouping:
  - auth actions together
  - update actions together
  - source preference actions together
  - debug action clearly separated as advanced/utility
- reduce the feeling of a long undifferentiated button stack

### Done when
- settings actions feel grouped and intentional, not just appended

## D3. Keep power-user affordances without broadcasting them everywhere

### Changes
- preserve debug and provider controls
- but visually subordinate low-frequency / expert actions
- avoid turning settings into a “developer page first, user page second” screen

### Done when
- power features remain intact, but product polish improves

---

# Phase E — Global focus, motion, spacing, and component consistency

This phase should be treated as a consistency sweep for leftovers after the main screen passes, not as a giant restyling event. Some normalization work will naturally happen during Phases 0/A/B/C; Phase E is for whatever still feels visibly inconsistent afterward.

## Goal

Make the app feel more intentionally polished across screens, not just individually improved.

## E1. Normalize focus behavior

### Changes
- review and align focus responses across:
  - rail buttons
  - home cards
  - poster cards
  - episode cards
  - source cards
  - settings buttons
- ensure scale, elevation, alpha, and text-color changes feel like one system
- avoid over-animating or using noticeably different focus semantics on similar elements

### Done when
- focus feels consistent enough that users stop noticing it

## E2. Normalize spacing and panel density

### Changes
- audit repeated spacing values in `ScreenRenderers.kt` / `ScreenViewFactory.kt`
- reduce ad hoc differences where similar sections use slightly different internal rhythms
- align panel padding, inter-section spacing, and button gaps toward a smaller set of standards

### Done when
- screens feel related and better composed without needing a redesign system rewrite

## E3. Review copy tone for TV

### Changes
- reduce over-explanatory helper copy where layout already communicates enough
- keep tips only where they prevent real confusion
- make labels shorter and more couch-legible

### Done when
- UI text supports action rather than narrating the app

---

# Suggested implementation slices

## Slice 0 — Card rendering/focus correction
- 0.1
- 0.2
- 0.3
- 0.4

## Slice 1 — Home premium pass
- A1
- A2
- A3

## Slice 2 — Details richness pass
- B1
- B2
- B3

## Slice 3 — Sources simplification
- C1
- C2
- C3

## Slice 4 — Settings cleanup
- D1
- D2
- D3

## Slice 5 — Global consistency pass
- E1
- E2
- E3

If scope or momentum tightens, the preferred ship line is:
- Slice 0
- Slice 1
- Slice 2
- Slice 3

with Slice 4 and Slice 5 treated as opportunistic follow-on polish rather than blocking work.

Each slice should be small enough to test visually on emulator before moving on.

---

# Validation plan

For each slice:

1. Build the app
   - `./gradlew assembleDebug`
2. Run relevant unit tests where affected
   - at minimum targeted tests or `testDebugUnitTest` when practical
3. Emulator validation with screenshots for changed screens
   - home
   - details
   - sources
   - settings
4. Confirm DPAD focus order manually on changed surfaces
5. Verify no regressions in:
   - search flow
   - favorites/history entry points
   - sources → player flow
   - back behavior

---

# Risks / watchouts

- Over-styling without improving hierarchy could make the app noisier rather than better.
- Home can easily become too decorative if shelves and actions lose clarity.
- Details can become art-heavy but less actionable if the hero treatment dominates too much.
- Sources must not lose important transparency while being simplified.
- Settings should improve visually without hiding critical maintenance actions.
- Since the UI is manually composed with Android Views, broad restyling can create subtle focus regressions if done too aggressively.

---

# Explicit non-goals

- No Compose migration in this pass
- No RecyclerView rewrite in this pass
- No broad navigation/state-architecture overhaul in this pass
- No backend/provider strategy change in this pass
- No major player chrome rewrite in this pass

---

# Definition of success

This pass is successful if:

- Home feels like a streaming-app landing page
- Details feels like a content destination, not just metadata
- Sources feels easier to trust and faster to act on
- Settings remains powerful but cleaner
- Focus and spacing feel more unified app-wide
- The app still feels lightweight and practical, just sharper
