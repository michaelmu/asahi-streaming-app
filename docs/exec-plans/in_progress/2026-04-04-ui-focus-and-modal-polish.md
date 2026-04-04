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

**Immediate target:** audit and improve focus styling plus modal focus trapping/default selection behavior.

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
Status: TODO
Priority: High

### Goal
Make focused buttons/cards feel deliberate and readable on TV without clipping or awkward scale artifacts.

### Why this matters
TV UIs live or die on focus clarity.
If focus only slightly scales and clips against rounded surfaces, it feels cheap immediately.

### Proposed sub-steps
- [TODO] inspect `ScreenViewFactory.button()` and panel/card focus behavior for clipping causes
- [TODO] reduce corner roundness where needed to avoid visible clipping during focus scale
- [TODO] reduce or rebalance focus scale if the current scale is too aggressive for the shape treatment
- [TODO] add stronger focused-state color change for primary selectable elements
- [TODO] keep focused state readable across dark backgrounds and elevated panels

### Validation
- emulator manual check on navigation rail, search buttons, settings buttons, and modal buttons
- screenshot capture for before/after focused buttons/cards
- no obvious clipping on focused state

---

## A2. Make selected items visually distinct beyond scale
Status: TODO
Priority: High

### Goal
Ensure selected/focused items in content lists clearly change color or styling, not just size.

### Why this matters
In lists like movies/results/sources/episodes, scale alone is weak TV feedback.
Users should be able to tell selection state instantly from across the room.

### Proposed sub-steps
- [TODO] review focus handlers in `ScreenRenderers.kt`
- [TODO] add focused-state color changes for result/source/episode cards where appropriate
- [TODO] ensure selected chips/season controls/readouts also communicate selection more strongly
- [TODO] keep cache-status and quality colors readable after focus styling updates

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
Status: TODO
Priority: High

### Goal
Make modals behave like proper TV overlays: focused by default, navigable internally, and not leaking focus to background content.

### Why this matters
Current behavior where left/right can move in and out of the modal is a direct TV UX break.
It makes the modal feel unfinished and confusing.

### Proposed sub-steps
- [TODO] inspect `OverlayPopup` focus handling and root/card/button container behavior
- [TODO] trap focus inside the modal while it is visible
- [TODO] ensure directional navigation cycles or stops within sensible modal bounds
- [TODO] verify back/dismiss behavior still works as intended
- [TODO] confirm background content is not focusable while modal is active

### Validation
- emulator manual check for:
  - source progress modal
  - info modal with 1 button
  - info modal with 2 buttons
  - info modal with 3 buttons
- screenshots of focused default button in modal states

---

## B2. Improve default modal button selection behavior
Status: TODO
Priority: High

### Goal
Make the intended modal action focused by default every time, consistently.

### Why this matters
`ModalDefaultAction` already exists, so the app clearly wants this behavior.
The current implementation just needs to be made more reliable and consistent.

### Proposed sub-steps
- [TODO] audit all `showInfoModal(...)` call sites in `MainActivity`
- [TODO] confirm each modal’s desired default action
- [TODO] tighten `OverlayPopup` so the default button reliably takes focus on show
- [TODO] review special cases like source loading / auth timeout / update/install flows

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

---

## Scope Changes

### 2026-04-04
- New plan created specifically for UI polish instead of overloading the device-hardening/shell-cleanup plan.
- Emulator + screenshot validation was made part of the required validation, not an optional extra.

---

## Session Start

### 2026-04-04 16:46 UTC
Intended task: create a dedicated UI polish execution plan grounded in the actual TV UI/focus/modal implementation seams.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted UI polish items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- emulator/manual validation is recorded
- screenshot validation is captured or explicitly documented if unavailable
