# UI Validation — 2026-04-04 Focus / Modal Polish

This directory is reserved for emulator/manual validation artifacts for the UI polish pass.

## Intended artifacts

Add screenshots here for:
- focused primary button
- focused rail item
- focused result card
- focused source card
- focused episode row
- 1-button modal default focus
- 2-button modal default focus
- 3-button modal default focus
- source progress modal with correct default focus and no focus leakage

## Suggested filenames
- `home-focused-button.png`
- `rail-focused-item.png`
- `results-focused-card.png`
- `sources-focused-card.png`
- `episodes-focused-row.png`
- `modal-one-button.png`
- `modal-two-button.png`
- `modal-three-button.png`
- `modal-source-progress.png`

## Emulator commands

If running locally with a working display/plugin setup:

```bash
emulator -avd asahi-tv-test -no-snapshot -no-audio -no-boot-anim
adb wait-for-device
until adb shell getprop sys.boot_completed | grep -q 1; do sleep 2; done
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Screenshot command

```bash
adb exec-out screencap -p > docs/ui-validation/2026-04-04-focus-modal-polish/<name>.png
```

## Manual validation checklist

### Focus styling
- Home screen: verify focused items do not visibly clip on rounded corners
- Search / results: verify focused cards change color/state clearly, not just scale
- Episode list: verify focused row is visibly distinct and selected row remains readable
- Sources list: verify focused source cards are obvious and quality/cache chips remain readable
- Settings: verify focused buttons are clear and not over-rounded/clipped

### Modal behavior
- Source progress modal: left/right should stay inside modal buttons
- Auth failure modal: intended default action should be focused immediately
- Auth link modal: intended default action should be focused immediately
- Update available modal: intended default action should be focused immediately
- Update installer modal(s): focus should stay inside modal and default action should match intent
- Provider selection modal(s): focus should remain trapped and default action should be sensible

## Current blocker in this environment

Attempted emulator launch from the current environment failed because the Android emulator could not initialize the Qt xcb platform plugin / display backend.

This means screenshots and live D-pad validation could not be completed here yet.
