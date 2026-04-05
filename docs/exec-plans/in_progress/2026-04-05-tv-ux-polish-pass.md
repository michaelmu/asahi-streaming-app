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

1. **Home still feels partly like a utility dashboard**
   - shelves exist, but lower sections still feel panel-driven rather than entertainment-led
   - featured/quick-pick area does not yet fully sell “browse from the couch”

2. **Some surfaces remain too text-heavy**
   - details, settings, and sources still lean heavily on explanatory text panels
   - TV UI should privilege artwork, hierarchy, and action confidence over paragraphs

3. **Sources flow is readable but not yet elegant**
   - current UI is good for power-user inspection
   - default “pick something good and play” path could feel more confident and lighter

4. **Details screen is functional, not yet premium**
   - structure is sound, but overall visual hierarchy is still conventional
   - more emotional/visual pull would help selection confidence

5. **Settings still reads as developer-utility UI**
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

1. **Home premium pass**
2. **Details screen richness pass**
3. **Sources default-path simplification**
4. **Settings cleanup pass**
5. **Global focus/motion/spacing consistency pass**

Reasoning:
- Home and details have the biggest impact on perceived product quality.
- Sources has major UX value, but should be adjusted after home/details visual language is clearer.
- Settings matters, but should not lead the pass.
- Global polish should happen after the primary screen changes establish the intended visual direction.

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

### Likely implementation
- evolve `DetailsScreenRenderer.render(...)`
- potentially reuse `viewFactory.artworkHero(...)` or add a details-specific hero block in `ScreenViewFactory`

### Done when
- details feels like a content destination, not just an information page

## B2. Rebalance metadata density

### Changes
- keep key metadata highly scannable:
  - type
n  - year
  - runtime / seasons
- reduce visual weight of secondary explanatory text
- ensure overview remains readable at TV distance without becoming a wall of text
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
