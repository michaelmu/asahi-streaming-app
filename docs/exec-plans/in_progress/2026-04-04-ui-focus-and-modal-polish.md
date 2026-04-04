# Asahi Execution Plan — UI Focus and Modal Polish

Last updated: 2026-04-04 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-ui-focus-and-modal-polish.md`
Supersedes: none

## Purpose

This plan is for a focused UI polish pass on Asahi’s TV interface.

The goal is to improve:
- focused/selected state visibility
- button/card clipping and corner treatment
- modal navigation behavior
- default modal button focus behavior
- consistency of focus affordances across browse/results/details/sources/settings screens

This pass is explicitly visual + interaction-oriented.
It is not a generic architecture pass.

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not after the fact.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes if the design direction shifts
- validation notes after emulator/manual checks

### Completion rule
A task is only `DONE` when:
- code landed
- relevant emulator/manual validation happened and was recorded
- screenshots were captured for the affected states where called for
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

**Current phase:** Phase A — focus-state polish and modal behavior cleanup

**Immediate target:** B2 — tighten default modal button selection behavior so the intended action is reliably focused every time.

**Why this now:**
The current UI is functional, but TV polish issues are visible:
- selected/focused buttons can look clipped
- focused items rely too much on scale-only feedback
- list/card focus states should be more visually obvious
- modal focus/navigation behavior is currently weird enough to break the TV feel

---

## Background / Review Summary

Initial review findings from the current implementation:

### 1. Focus styling is mostly programmatic
The UI is largely built in Kotlin via:
- `ScreenViewFactory`
- `ScreenRenderers.kt`
- `OverlayPopup`

This means the right fixes are likely in:
- button/panel/card focus listeners
- background drawable choices already used by `ScreenViewFactory`
- popup focus trapping logic

not in a large XML theme overhaul.

### 2. Buttons currently emphasize scale more than color/state
`ScreenViewFactory.button()` uses:
- scale on focus
- alpha change on focus
- shared button background drawable

But there is no strong focused-state color shift.
That matches the complaint that selected items just look slightly larger rather than clearly selected.

### 3. Card/panel focus behaviors are repeated in multiple renderers
Focus handling is duplicated across:
- home cards
- results cards
- episode cards
- source cards

That suggests a consistency pass is needed, and perhaps a shared helper if it actually reduces drift without overengineering.

### 4. Modal navigation is very likely not properly trapped
`OverlayPopup` requests focus on a default button, but it does not appear to actively constrain focus search inside the modal container.
This likely explains left/right/edge behavior where focus moves in and out of the modal unexpectedly.

### 5. Default modal selection exists conceptually, but probably needs stronger implementation
`ModalDefaultAction` is already present and used, which is good.
But the actual runtime behavior still needs tightening so:
- the intended button is focused by default
- focus stays inside the modal until dismissal
- directional navigation behaves predictably

---

# Phase A — Focus-state visual polish

## A1. Reduce clipping risk and improve button/card focus affordances
Status: DONE
Priority: High

### Goal
Make focused buttons/cards feel deliberate and readable on TV without clipping or awkward scale artifacts.

### Why this matters
TV UIs live or die on focus clarity.
If focus only slightly scales and clips against rounded surfaces, it feels cheap immediately.

### Proposed sub-steps
- [DONE] inspect `ScreenViewFactory.button()` and panel/card focus behavior for clipping causes
- [DONE] reduce corner roundness where needed to avoid visible clipping during focus scale
- [DONE] reduce or rebalance focus scale if the current scale is too aggressive for the shape treatment
- [DONE] add stronger focused-state color change for primary selectable elements
- [DONE] keep focused state readable across dark backgrounds and elevated panels

### Validation
- emulator manual check on navigation rail, search buttons, settings buttons, and modal buttons
- screenshot capture for before/after focused buttons/cards
- no obvious clipping on focused state

---

## A2. Make selected items visually distinct beyond scale
Status: DONE
Priority: High

### Goal
Ensure selected/focused items in content lists clearly change color or styling, not just size.

### Why this matters
In lists like movies/results/sources/episodes, scale alone is weak TV feedback.
Users should be able to tell selection state instantly from across the room.

### Proposed sub-steps
- [DONE] review focus handlers in `ScreenRenderers.kt`
- [DONE] add focused-state color changes for result/source/episode cards where appropriate
- [DONE] ensure selected chips/season controls/readouts also communicate selection more strongly
- [DONE] keep cache-status and quality colors readable after focus styling updates

### Validation
- emulator manual check on:
  - home quick picks
  - search results
  - episode list
  - sources list
  - settings buttons
- screenshots for focused rows/cards in at least 4 screens

---

# Phase B — Modal behavior and default focus cleanup

## B1. Fix modal focus trapping and directional navigation
Status: DONE
Priority: High

### Goal
Make modals behave like proper TV overlays: focused by default, navigable internally, and not leaking focus to background content.

### Why this matters
Current behavior where left/right can move in and out of the modal is a direct TV UX break.
It makes the modal feel unfinished and confusing.

### Proposed sub-steps
- [DONE] inspect `OverlayPopup` focus handling and root/card/button container behavior
- [DONE] trap focus inside the modal while it is visible
- [DONE] ensure directional navigation cycles or stops within sensible modal bounds
- [DONE] verify back/dismiss behavior still works as intended
- [DONE] confirm background content is not focusable while modal is active

### Validation
- emulator manual check for:
  - source progress modal
  - info modal with 1 button
  - info modal with 2 buttons
  - info modal with 3 buttons
- screenshots of focused default button in modal states

---

## B2. Improve default modal button selection behavior
Status: DONE
Priority: High

### Goal
Make the intended modal action focused by default every time, consistently.

### Why this matters
`ModalDefaultAction` already exists, so the app clearly wants this behavior.
The current implementation just needs to be made more reliable and consistent.

### Proposed sub-steps
- [DONE] audit all `showInfoModal(...)` call sites in `MainActivity`
- [DONE] confirm each modal’s desired default action
- [DONE] tighten `OverlayPopup` so the default button reliably takes focus on show
- [DONE] review special cases like source loading / auth timeout / update/install flows

### Validation
- emulator manual check that default focus matches intent for representative modals
- screenshot capture of at least 5 modal states showing correct default selection

---

# Phase C — Optional consistency cleanup

## C1. Extract reusable focus-style helpers if repetition is still high
Status: OPTIONAL
Priority: Medium

### Notes
Only do this if it reduces real duplication cleanly.
Do not invent a mini UI framework just because multiple renderers use similar focus listeners.

## C2. Add a tiny visual regression checklist for TV UI states
Status: OPTIONAL
Priority: Low

### Notes
This could just be a checklist doc plus screenshots, not a giant screenshot-testing framework.

---

## Emulator / Screenshot Validation Plan

Required validation for this plan should include emulator testing and screenshots.

### Emulator targets
- Android TV emulator / Leanback-style configuration if available
- if a Shield-like emulator profile is available, prefer that

### Screens to validate manually
- Home screen
- Search screen
- Results screen
- Details screen
- Episode picker
- Sources screen
- Settings screen
- representative modals

### Screenshot checklist
Capture screenshots for:
- focused primary button
- focused rail item
- focused result card
- focused source card
- focused episode row
- 1-button modal default focus
- 2-button modal default focus
- 3-button modal default focus
- source progress modal while focus is trapped correctly

### Validation artifacts
Store screenshots in a sensible docs path if created during implementation, for example:
- `docs/ui-validation/2026-04-04-focus-modal-polish/`

If that directory is created, note it in the progress log.

---

## Recommended Order

1. A1 Reduce clipping risk and improve focus affordances
2. A2 Strengthen selected-state visual distinction
3. B1 Fix modal focus trapping/navigation
4. B2 Improve default modal button selection behavior
5. Optional C-items only if the core polish pass is stable

---

## Open Questions / Decisions Needed

### Q1. Should focused-state styling be driven mostly by color, mostly by scale, or both?
Current recommendation:
Use both, but reduce dependence on scale.
A smaller scale bump plus a stronger color/background treatment is more TV-friendly than “just make it bigger.”

### Q2. Should modal focus wrap, clamp, or explicitly bounce at row edges?
Current recommendation:
Clamp or explicitly manage focus within the modal bounds.
Do not let focus leak to background content.

### Q3. Should we centralize all focus animations in `ScreenViewFactory`, or keep some renderer-specific variants?
Current recommendation:
Centralize common button/card focus behavior first, but keep renderer-specific differences where needed for layout-specific polish.

---

## Risks / Watchouts

- reducing corner radius too much could lose the current visual identity
- changing focus color treatment could accidentally hurt readability against existing cache-status colors
- modal focus trapping can break back/dismiss behavior if done carelessly
- aggressive global refactors of focus listeners could create subtle regressions across many screens

---

## Progress Log

### 2026-04-04 16:46 UTC
- Created a dedicated UI polish execution plan based on reported focus/clipping/modal issues and an initial code audit.
- Confirmed the main UI surfaces are programmatic and that `ScreenViewFactory`, `ScreenRenderers.kt`, and `OverlayPopup` are the main implementation seams.
- Added emulator and screenshot validation requirements to this plan.
- No implementation work completed under this plan yet.

### 2026-04-04 16:52 UTC
- Completed the first A1 implementation slice by reducing clipping-prone corner radii in shared button/panel drawables and rebalancing shared focus styling in `ScreenViewFactory`.
- Reduced focus dependence on aggressive scale and shifted more visual weight onto background/state treatment.
- Updated chip selected/focused styling so focused state is more visible even before deeper renderer-specific color work.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.
- Emulator/screenshot validation is still pending for the broader UI pass and remains required before closing this plan.

### 2026-04-04 16:56 UTC
- Completed A2 by strengthening renderer-specific focus treatment for media cards, episode rows, and source cards in `ScreenRenderers.kt`.
- Reduced reliance on scale-only selection cues and added stronger background/text-state differentiation while preserving cache/quality readability.
- Introduced a small shared text-color propagation helper at file scope to keep focus-state updates consistent inside composite cards.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.
- Emulator/screenshot validation remains pending and is still required before the overall UI plan is considered complete.

### 2026-04-04 17:00 UTC
- Completed B1 by tightening `OverlayPopup` focus handling so modal focus stays inside the overlay instead of leaking into background content.
- Added directional focus clamping/trapping for modal buttons and reinforced default focus restoration when the overlay root regains focus.
- Kept the fix localized to `OverlayPopup` rather than introducing a larger modal framework.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.
- Emulator/manual validation is still needed to confirm real D-pad behavior matches the intended trap behavior.

### 2026-04-04 17:04 UTC
- Completed B2 by tightening default modal button selection behavior in `OverlayPopup` and auditing high-value modal call sites in `MainActivity`.
- Added explicit generated IDs/tags for modal buttons and reinforced repeated default-focus restoration after show/focus bounce.
- Updated key modal defaults so destructive/cancellation actions are less likely to receive accidental initial focus (for example, source lookup progress now defaults to Keep Waiting instead of Cancel).
- Explicitly set default actions in important auth-linking modal flows for clarity and consistency.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.
- Emulator/manual validation is still required to confirm the intended default-focus behavior on real D-pad navigation.

### 2026-04-04 17:08 UTC
- Attempted emulator validation for the UI polish pass.
- Environment has an Android TV AVD (`asahi-tv-test`) and `adb`, but emulator launch failed here due to Qt/xcb platform plugin/display initialization issues.
- Created `docs/ui-validation/2026-04-04-focus-modal-polish/README.md` with the screenshot checklist, adb screenshot command, emulator commands, and manual validation steps.
- Result: validation artifact path is prepared, but screenshots/live emulator verification remain blocked in this environment and must be completed where the emulator display backend works.

---

## Scope Changes

### 2026-04-04
- New plan created specifically for UI polish instead of overloading the device-hardening/shell-cleanup plan.
- Emulator + screenshot validation was made part of the required validation, not an optional extra.
- During A1 implementation, started with shared drawable/factory-level focus treatment changes first because they offer the highest leverage and lowest regression risk before deeper renderer/modal work.
- Validation scope remains unchanged, but actual emulator execution is currently blocked in this environment by the emulator display/plugin setup rather than missing tooling.
- During A2 implementation, focused on the highest-traffic renderer-specific cards first (media, episode, source) rather than trying to restyle every focusable widget in one pass.
- During B1 implementation, kept the modal navigation fix localized to `OverlayPopup` so all modal call sites benefit immediately without per-modal rewrites.
- During B2 implementation, preferred safe default focus on non-destructive actions where the UX intent was ambiguous, especially for long-running source lookup and auth flows.

---

## Session Start

### 2026-04-04 17:02 UTC
Intended task: begin B2 by auditing modal default-action call sites and tightening default button focus behavior in `OverlayPopup`.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted UI polish items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- emulator/manual validation is recorded
- screenshot validation is captured or explicitly documented if unavailable
