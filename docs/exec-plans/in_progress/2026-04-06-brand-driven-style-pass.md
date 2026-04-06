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
- record each token with:
  - token/resource name
  - hex value
  - semantic role
  - allowed usage
  - banned usage
  - existing resource(s) or drawable touchpoints to remap

#### 0B. Define component-level styling rules
- specify how buttons, panels, chips, cards, inputs, focus rings, progress bars, and modals should use the new tokens
- define which surfaces should stay neutral so content art still pops
- document when to use coral vs amber vs plum to avoid inconsistent theming
- note where existing semantic resource names should be remapped rather than replaced with one-off colors

#### 0C. Define explicit TV focus rules
- specify the focused-state recipe for each major control family: nav items, buttons, chips, cards, inputs, list rows, and modal actions
- decide, per family, which focus signals are allowed: stroke/ring, fill shift, scale, glow, elevation, text-color shift, badge reveal
- define one default focus approach for content cards and one default focus approach for control surfaces; avoid one-off focus inventions per screen
- document minimum contrast expectations for focused vs unfocused states on warm surfaces
- treat focus clarity as a functional requirement, not a decorative choice

#### 0D. Produce a small implementation artifact
- append a compact token table directly to this plan before implementation begins
- include, for each token: name, hex, role, allowed usage, banned usage, remap targets
- append a compact component-rules table covering buttons, panels, inputs, chips, cards, modals, progress, and nav items
- call out any intentionally neutral surfaces that should resist branding pressure

#### 0E. Add a “brand acceptance” checklist
- from 10 feet away, text remains readable
- focused element is always unmistakable
- artwork still feels primary on browse surfaces
- settings/source screens feel branded, not merely recolored
- the app feels intentionally styled, not over-skinned

#### 0F. Append implementation tables before coding begins

##### Token table stub

| Token name | Hex | Role | Allowed usage | Banned usage | Existing resources / drawables to remap |
| --- | --- | --- | --- | --- | --- |
| `asahi_color_bg_app` | `#151927` | app background | root app background, large negative space, launcher background | focused outlines, warning states, CTA fills | current `@color/asahi_bg`; `themes.xml` window/status/nav bars; `asahi_app_bg.xml`; launcher backgrounds; `ScreenViewFactory.backgroundColor` |
| `asahi_color_surface_1` | `#20253A` | base panel surface | standard panels, rail containers, neutral control fills | semantic warning/success/error, hero text directly over imagery | current `@color/asahi_panel`; `asahi_panel_bg.xml`; `asahi_chip_bg.xml` default; `asahi_input_bg.xml` default; `ScreenViewFactory.panelColor` |
| `asahi_color_surface_2` | `#2A314D` | elevated panel surface | dialogs, elevated panels, secondary buttons, selected neutral containers | global page background, default focus indicator | current `@color/asahi_panel_elevated`; `asahi_panel_elevated_bg.xml`; `asahi_button_bg.xml` default/pressed; `asahi_input_bg.xml` focused; `asahi_poster_card_bg.xml` default/pressed; launcher banner panel; `ScreenViewFactory.panelElevatedColor` |
| `asahi_color_surface_3` | `#343D5F` | strongest neutral surface | pressed/active neutral fills, utility grouping, stronger non-primary control emphasis | app-wide default fill for all components, semantic states | current `@color/asahi_panel_soft`; `asahi_button_bg.xml` pressed; `asahi_chip_bg.xml` focused |
| `asahi_color_accent_primary` | `#F06B5F` | branded primary accent | primary CTAs, selected chips, branded emphasis, progress fills | body text, warning/error semantics, blanket poster tinting | current `@color/asahi_accent`; `themes.xml` `colorPrimary`; `asahi_button_bg.xml` focused; `asahi_chip_bg.xml` selected; `asahi_progress_fill.xml`; `ScreenViewFactory.accentColor` |
| `asahi_color_accent_secondary` | `#F3A64F` | secondary accent | gradient support, secondary highlights, warm supporting accents | primary focus ring, long-form text, warning/destructive semantics | current `@color/asahi_accent_alt`; `themes.xml` `colorSecondary`; `asahi_progress_fill.xml`; `ScreenViewFactory.accentAltColor` |
| `asahi_color_accent_selected` | `#6B4B7D` | selected-state accent base | selected navigation/buttons where plum is intended to signal active state | generic panel fill, warning/destructive actions, text roles | current `@color/asahi_accent_selected`; `asahi_button_selected_bg.xml` default |
| `asahi_color_accent_selected_focus` | `#8A5FA1` | focused selected-state accent | focused selected navigation/buttons where active + focused should stack clearly | unfocused default fills, semantic status colors | current `@color/asahi_accent_selected_focus`; `asahi_button_selected_bg.xml` focused |
| `asahi_color_focus` | `#F08A7E` | focus indicator | focused strokes, rings, high-confidence DPAD focus cues across controls | passive decoration, unfocused borders, semantic warning use | current `@color/asahi_focus`; button/chip/input/poster focused strokes; selected-button focused stroke |
| `asahi_color_warning` | `#F3C76A` | warning/caution semantic | warnings, caution copy accents, non-destructive attention states | generic branding, default CTA styling | current `@color/asahi_warning`; `ScreenViewFactory.warningColor` |
| `asahi_color_error` | `#F06B5F` | destructive/error semantic | destructive states, errors, failing states | general branding when semantic meaning is not intended | current `@color/asahi_error`; `ScreenViewFactory.errorColor` |
| `asahi_color_text_primary` | `#FFF6EE` | primary text | titles, primary body text, important labels | disabled text, subtle chrome, heavy accent fills | current `@color/asahi_text_primary`; `ScreenViewFactory.textPrimaryColor` |
| `asahi_color_text_secondary` | `#D8C6BF` | secondary text | metadata, helper copy, supporting labels | critical warnings, primary CTA fills | current `@color/asahi_text_secondary`; `ScreenViewFactory.textSecondaryColor` |
| `asahi_color_text_tertiary` | `#AE9EA0` | tertiary/quiet text | subdued metadata, lower-priority helper text, quiet status lines | important focus labels, destructive/semantic copy, CTA labels | current `@color/asahi_text_muted`; `ScreenViewFactory.textMutedColor` |
| `asahi_color_stroke_neutral` | `#4C557B` | unfocused neutral stroke | panel borders, unfocused control outlines, subtle separation | focused indicators, semantic highlights | current `@color/asahi_stroke`; panel/button/chip/input/poster default strokes; launcher banner stroke |
| `asahi_color_scrim` | `#CC11141F` | scrim/overlay | modal backdrop, overlay dimming, readability support behind layered surfaces | default panel fill, focus ring, accent usage | current `@color/asahi_scrim`; `ScreenViewFactory` overlay scrim |
| `asahi_color_card_focus_fill_candidate` | `#141A24` | focused poster-card fill candidate | temporary/fallback focused poster surface if artwork needs protected framing | general panel usage, CTA fills, text usage | current hardcoded fill in `asahi_poster_card_bg.xml`; should likely become a named color token before implementation |

##### Component rules table stub

| Component family | Default surface token(s) | Default text token(s) | Accent usage | Focus recipe | Notes / neutrality rules |
| --- | --- | --- | --- | --- | --- |
| App shell / root canvas | `asahi_color_bg_app` | `asahi_color_text_primary`, `asahi_color_text_secondary` | minimal | none on static shell; focus belongs to active child controls | maps to `asahi_app_bg.xml`, theme window/status/nav background, and `MainActivity` root background; keep neutral enough that content and dialogs pop |
| Left rail / navigation | `asahi_color_surface_1` with selected state using `asahi_color_accent_selected` / `asahi_color_accent_selected_focus` | `asahi_color_text_primary`, `asahi_color_text_secondary` | selected/active item may use plum active-state accent; avoid making whole rail loud | focused item uses explicit focus ring/fill shift with high contrast | currently built via `focusableButton(...)` in `ScreenRenderers.kt` and backed by `asahi_button_bg.xml` / `asahi_button_selected_bg.xml`; should feel branded, not louder than content area |
| Panels / containers | `asahi_color_surface_1` / `asahi_color_surface_2` | text primary/secondary | accent only for intentional highlights | focus only if container itself is actionable | maps to `asahi_panel_bg.xml`, `asahi_panel_elevated_bg.xml`, `MainActivity` panel surfaces, and `ScreenViewFactory.panel(...)`; default to neutral branded surfaces |
| Buttons — primary | `asahi_color_accent_primary` over neutral surface context | `asahi_color_text_primary` or contrast-safe label color | primary accent is allowed | focused state may combine scale + ring + fill shift | currently `asahi_button_bg.xml` focused state is acting like a primary/focused treatment; verify whether unfocused primary CTA needs its own drawable or semantic variant |
| Buttons — secondary | `asahi_color_surface_2` | `asahi_color_text_primary` | accent optional, restrained | focused state must still be unmistakable on warm fills | currently default `asahi_button_bg.xml`; avoid making all buttons equally loud |
| Chips / pills / badges | `asahi_color_surface_1` by default, `asahi_color_accent_primary` when selected | text primary/secondary | secondary accent may support but current implementation uses primary accent | focused chip uses same family focus rule, not bespoke styling | maps to `asahi_chip_bg.xml`; keep small elements legible and not candy-colored |
| Inputs / search fields | `asahi_color_surface_1` / `asahi_color_surface_2` | text primary/secondary/tertiary by state | accent only for active cursor/selection/helpful affordance | focused input must be obvious even before text entry | maps to `asahi_input_bg.xml` and `ScreenViewFactory.input(...)`; utility clarity beats decorative styling |
| Content cards / poster cards | near-neutral card surround using `asahi_color_surface_2` or `asahi_color_card_focus_fill_candidate` | text on supporting metadata only | minimal accent; do not tint artwork | focused card uses one standard card recipe (ring/stroke + scale/elevation + optional metadata reveal) | maps to `asahi_poster_card_bg.xml` and multiple poster-card sites in `ScreenRenderers.kt`; artwork stays primary, branding supports rather than competes |
| Progress / status bars | neutral track + branded or semantic fill | n/a | accent or semantic fill as appropriate | focus only if directly actionable | maps to `asahi_progress_bg.xml`, `asahi_progress_fill.xml`, and `ScreenViewFactory.progress(...)`; preserve semantic meaning |
| Modals / dialogs / pickers | `asahi_color_surface_2` / `asahi_color_surface_3` with `asahi_color_scrim` backdrop | text primary/secondary | selective accent on primary actions only | focused actions/rows use shared control focus recipe | maps to overlay scrim in `ScreenViewFactory` and elevated panel surfaces; layered surfaces should still read above shell |
| Details hero overlays | likely darkened neutral overlay derived from `asahi_color_scrim` / `asahi_color_bg_app` | high-contrast text tokens | very restrained accent | focus applies to actions, not decorative hero region | currently assembled in `ScreenRenderers.kt` details surfaces; backdrop readability wins over brand expressiveness |
| Settings / sources / utility screens | `asahi_color_surface_1` / `asahi_color_surface_2` | text primary/secondary/tertiary | restrained accent to reduce “tooling” feel | focus must remain stronger than branding | primarily `ScreenRenderers.kt` settings/source flows plus `ScreenViewFactory` buttons/panels/inputs; should feel trustworthy, crisp, and not fuzzy |

##### Focus rules table stub

| Control family | Allowed focus signals | Disallowed focus signals | Minimum expectation |
| --- | --- | --- | --- |
| Navigation items | ring/stroke, fill shift, mild scale, text emphasis | subtle tint-only focus with no shape/value change | instantly identifiable in peripheral vision |
| Buttons / action rows | ring/stroke, fill shift, mild scale/elevation | focus that depends only on color temperature change | obvious from couch distance before label is read |
| Chips / pills | ring/stroke, fill shift, text emphasis | tiny glow-only treatment | readable without looking decorative |
| Inputs | ring/stroke, fill shift, cursor/selection accent | relying only on caret visibility | obvious when ready for typing |
| Content cards | ring/stroke, scale/elevation, optional metadata reveal | heavy artwork tinting, decorative overlays that muddy art | unmistakable while preserving poster fidelity |
| Modal choices / list rows | ring/stroke, fill shift, text emphasis | ambiguous low-contrast focus | clear within layered surfaces |

**Exit criteria:** there is a clear token/system map, including explicit focus behavior, before wide visual churn begins.

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
- keep this limited to rhythm/polish changes such as margins, gaps, row separation, padding, and grouping cues
- do not change navigation behavior, IA, or core information density here unless a styling dependency clearly forces it

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
   - sources / resolver flows
   - settings/modal flows
4. verify DPAD focus visibility on every touched control type
5. verify both focused and unfocused states on the same touched screens
6. check at least one low-artwork or missing-artwork scenario
7. check at least one long-title / long-subtitle scenario on a touched browse or details surface
8. check at least one dense screen where many controls or cards are simultaneously visible
9. check at least one layered state such as modal-over-shell or dialog-over-content
10. compare against the source image and ask:
   - does this feel recognizably derived from the brand image?
   - is readability still strong from TV distance?
   - is content artwork still more important than decorative styling?
   - does focus still win instantly against warm surfaces?

## Practical implementation order

Recommended order:
1. Slice 0 — define token system, component rules, and focus rules
2. Slice 1 — global colors + shared drawables
3. Slice 2 — shell/rail/chrome
4. Slice 3 — browse/card pass using the updated primitives
5. Slice 4 — buttons/inputs/modals/settings controls
6. Slice 5 — details/sources polish
7. Slice 6 — asset refinement and final sweep

Note: if launcher/adaptive-icon crop quality is still unknown or clearly weak, pull Slice 6A forward immediately after Slice 0 so asset treatment can inform token decisions rather than lag behind them.

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
- the UI feels intentionally branded without feeling over-skinned, fuzzy, or decorative at the expense of scanability
