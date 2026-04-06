# 2026-04-06 — Brand-driven style pass

## Objective

Use the shared brand image as the design anchor for a broader visual style pass across the TV app, not just the launcher icon. The goal is to make Asahi feel intentionally branded around the image’s warm coral / amber / plum / cream palette and soft retro-friendly personality while still reading clearly from couch distance on Android TV.

## Why this pass exists

The image is doing more than supplying an icon:
- it introduces a warmer, friendlier tone than the current cool blue-heavy palette
- it suggests softer contrast relationships and a less clinical visual identity
- it gives the app a recognizable personality that can unify iconography, focus states, surfaces, and copy tone

The current app only partially reflects that shift:
- launcher and banner art can carry the image directly
- the app palette can borrow from the image
- but many UI surfaces still behave like the earlier colder system and haven’t been rebalanced around the new brand language yet

## Design intent

Translate the image into a stable TV UI language:
- **Mood:** warm, playful, low-drama, cozy-tech rather than sharp enterprise blue
- **Primary colors:** coral, amber, plum, cream, deep inky background
- **Surface behavior:** softer panels, less harsh contrast jumps, less “utility console” feel
- **Focus behavior:** still obvious for DPAD, but more integrated with the brand colors instead of generic system blue
- **Typography/copy tone:** concise, calm, a little warmer, without becoming childish

## Constraints

- Preserve TV readability at distance first
- Do not reduce focus visibility just to match the image
- Do not over-theme content artwork; the app still needs to showcase movie/show art cleanly
- Keep implementation compatible with the current XML/View-based UI stack
- Prefer small, composable changes over one giant visual rewrite
- Prefer shared primitive/style changes before per-screen overrides
- Avoid mixing broad palette work with unrelated layout or interaction rewrites unless necessary

## Out of scope

- no navigation model rewrites
- no deep information architecture changes
- no major interaction-model changes for DPAD flows
- no risky playback/controller refactors
- no decorative treatments that reduce poster or backdrop fidelity

## Execution slices

### Slice 0 — Brand system definition

#### 0A. Extract usable design tokens from the image
- identify 1 background tone, 2-3 surface tones, 1 primary accent, 1 secondary accent, 1 focus tone, 1 warning tone, and 3 text tones
- assign stable resource names for those tokens before implementation begins
- document which colors are decorative vs interactive vs semantic
- avoid using every image color everywhere; keep the token set small and reusable

#### 0B. Define component-level styling rules
- specify how buttons, panels, chips, cards, inputs, focus rings, progress bars, and modals should use the new tokens
- define which surfaces should stay neutral so content art still pops
- document when to use coral vs amber vs plum to avoid inconsistent theming
- note where existing semantic resource names should be remapped rather than replaced with one-off colors

#### 0C. Produce a small implementation artifact
- add a compact token table to this plan or a companion note
- map each token to likely XML resource names / drawable touchpoints
- call out any intentionally neutral surfaces that should resist branding pressure

#### 0D. Add a “brand acceptance” checklist
- from 10 feet away, text remains readable
- focused element is always unmistakable
- artwork still feels primary on browse surfaces
- settings/source screens feel branded, not merely recolored

**Exit criteria:** there is a clear token/system map before wide visual churn begins.

### Slice 1 — Core palette and surface rebalance

#### 1A. Rework global colors safely
- update `colors.xml` with stable brand tokens
- prefer remapping existing semantic/shared resource names to the new token system rather than proliferating one-off colors
- align app background, panel, elevated panel, stroke, scrim, and text hierarchy to the new palette
- make sure semantic colors still read correctly (warning/error/success if present)

#### 1B. Normalize shared drawables
- review and tune:
  - `asahi_app_bg.xml`
  - `asahi_panel_bg.xml`
  - `asahi_panel_elevated_bg.xml`
  - `asahi_button_bg.xml`
  - `asahi_button_selected_bg.xml`
  - `asahi_chip_bg.xml`
  - `asahi_input_bg.xml`
  - `asahi_poster_card_bg.xml`
  - progress drawables
- remove any leftover cold-blue assumptions where they conflict with the new system

#### 1C. Validate contrast and layering
- verify text on all shared surfaces
- verify focused state on warm surfaces does not disappear
- verify poster artwork is not muddied by warmer overlays

**Exit criteria:** the shared visual primitives feel like one branded system.

### Slice 2 — Navigation, shell, and chrome pass

#### 2A. Re-theme the left rail and shell framing
- make the side rail feel like part of the brand rather than a generic app scaffold
- tune selected/active navigation styling around the new palette
- ensure status text/build line still reads cleanly

#### 2B. Rebalance page headers and helper copy
- soften harsh contrast where needed
- make section titles, captions, and helper text feel cohesive with the new palette
- trim any helper copy that fights the cleaner branded presentation

#### 2C. Check shell-to-content transitions
- verify the rail, content canvas, and modal layers feel visually related
- avoid abrupt palette jumps between shell chrome and content surfaces

**Exit criteria:** the app frame itself expresses the new identity before deeper screen polish.

### Slice 3 — Browse and card styling pass

#### 3A. Revisit image-only card styling under the new brand
- tune card borders, scrims, elevation, and focus for the new palette
- ensure image-only cards still feel polished and not too bare
- consider subtle branded fallback placeholders when no artwork exists
- do not add decorative overlays that reduce poster fidelity or compete with artwork

#### 3B. Rebalance shelves and results spacing
- align spacing, shadows, and card grouping to the warmer visual language
- make shelves feel curated rather than just rows of assets
- preserve scanability and fast focus movement

#### 3C. Validate content-first behavior
- verify branding supports the content rather than competing with it
- keep artwork vivid and readable
- avoid heavy tinting or decorative overlays on media imagery

**Exit criteria:** browse surfaces feel branded but still clearly content-led.

### Slice 4 — Action/control styling pass

#### 4A. Buttons and inputs
- restyle primary/secondary buttons to match the new token rules
- make search inputs, modal choices, and action rows feel less utilitarian
- ensure hover/focus/pressed states remain obvious on TV

#### 4B. Modals, pickers, and settings controls
- bring settings and modal surfaces into the same brand language
- rebalance borders/fills/focus so control-heavy screens don’t feel visually detached
- verify picker flows still read clearly with remote navigation

#### 4C. Status and progress elements
- update progress bars, pills, tags, and badges to the new system
- ensure semantic meaning is still legible and not color-confused

**Exit criteria:** interactive controls feel as intentional as the browse surfaces.

### Slice 5 — Details / sources / player-adjacent polish

#### 5A. Details screen polish
- tune hero text overlays, stat pills, and action hierarchy to fit the brand system
- keep strong readability on backdrop imagery

#### 5B. Sources and utility-heavy screens
- reduce any remaining “tooling” look using the new surface and accent rules
- make source selection feel branded and trustworthy without becoming decorative

#### 5C. Player-adjacent overlays (if present in scope)
- check whether playback entry, loading, or transitional UI needs palette alignment
- only touch player-facing UI if it can be done safely without destabilizing playback flow
- do not change playback controller behavior or Media3 theming unless a purely cosmetic app-owned surface is clearly isolated

**Exit criteria:** high-information screens still feel like part of the same product family.

### Slice 6 — Brand asset follow-up

#### 6A. Launcher/icon refinement
- verify the provided image crops well for launcher + round launcher + banner
- if needed, create a cleaner adaptive-icon-safe composition derived from the image rather than relying on raw image placement alone

#### 6B. Optional branded placeholders
- add fallback art/placeholders derived from the brand language for missing posters, empty states, or low-data surfaces
- avoid overuse; placeholders should support, not dominate

#### 6C. Final copy/style consistency sweep
- ensure wording and visual tone match the warmer identity
- remove any stray surfaces still visually tied to the old palette

**Exit criteria:** the image is reflected consistently in both branding assets and runtime style.

## Validation plan

For each major slice:
1. `./gradlew assembleDebug testDebugUnitTest`
2. install to emulator and launch explicitly with `adb shell am start -n ai.shieldtv.app/.MainActivity`
3. capture screenshots for:
   - launcher/banner if feasible
   - home
   - search/results
   - details
   - sources
   - settings/modal flows
4. verify DPAD focus visibility on every touched control type
5. verify both focused and unfocused states on the same touched screens
6. check at least one low-artwork or missing-artwork scenario
7. compare against the source image and ask:
   - does this feel recognizably derived from the brand image?
   - is readability still strong from TV distance?
   - is content artwork still more important than decorative styling?

## Practical implementation order

Recommended order:
1. Slice 0 — define token system and component rules
2. Slice 1 — global colors + shared drawables
3. Slice 2 — shell/rail/chrome
4. Slice 3 — browse/card pass using the updated primitives
5. Slice 4 — buttons/inputs/modals/settings controls
6. Slice 5 — details/sources polish
7. Slice 6 — asset refinement and final sweep

Implementation discipline:
- keep commits slice-sized where practical
- verify emulator behavior after each slice before proceeding
- avoid combining broad visual changes with structural rewrites unless a dependency forces it

## Deliverables

- updated design-token palette in resources
- tuned shared drawables and surface styles
- runtime screenshots showing before/after style coherence
- refined launcher/banner treatment if raw image placement proves weak
- final note summarizing what from the source image was translated literally vs abstracted into the UI system

## Risks / watch-outs

- warm palettes can reduce focus clarity if contrast is not handled carefully
- image-driven branding can clash with content artwork if overlays become too decorative
- raw image-as-icon may look acceptable in-app but weak in launcher crops
- too much brand styling on settings/sources can hurt legibility or make power-user tasks feel fuzzy

## Definition of done

This pass is done when:
- the app feels recognizably inspired by the shared image even outside the launcher icon
- the shell, controls, and content surfaces all feel like one branded product
- focus and readability remain excellent on TV
- artwork-heavy browse screens still prioritize the content over the theme
- no touched screen still looks tied to the legacy blue-heavy system unless intentionally kept neutral
