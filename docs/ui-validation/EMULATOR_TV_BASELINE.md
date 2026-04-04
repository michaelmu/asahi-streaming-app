# Emulator TV Baseline

Recommended baseline for validating TV-first UI in `asahi-streaming-app`.

## Current preferred AVD

- Name: `asahi-tv-test`
- Device: `tv_1080p`
- Target: `android-34 / google-tv / x86`
- Resolution: `1920x1080`
- Density: `320`

## Why this baseline

This is not identical to an NVIDIA Shield, but it is close enough for:
- Home/menu visibility
- DPAD focus/navigation
- clipping/viewport issues
- favorites/history discoverability
- episode-list watched indicators
- modal behavior and focus loops

Do **not** treat it as final truth for playback/device-specific behavior.
Use the physical Shield for:
- playback rendering correctness
- hardware decode quirks
- final UX confirmation

## Recommended AVD settings

Prefer these values in `~/.android/avd/asahi-tv-test.avd/config.ini`:

- `hw.device.name=tv_1080p`
- `tag.id=google-tv`
- `hw.dPad=yes`
- `hw.lcd.width=1920`
- `hw.lcd.height=1080`
- `hw.lcd.density=320`
- `hw.initialOrientation=landscape`
- `hw.mainKeys=yes`
- `hw.keyboard=no`
- `hw.trackBall=no`
- `hw.rotaryInput=no`

## Launch command

```bash
QT_QPA_PLATFORM=offscreen /opt/android-sdk/emulator/emulator \
  -avd asahi-tv-test \
  -no-window \
  -gpu swiftshader_indirect \
  -no-snapshot \
  -no-audio \
  -no-boot-anim
```

## Validation flow

For TV UI changes, capture all of:
- home screenshot
- relevant list/detail screenshot(s)
- `uiautomator dump` XML
- short note about focus path if navigation behavior matters

## App-state seeding suggestions

For useful validation, prefer testing with:
- at least 2 favorites
- at least 2 watch-history entries
- at least 1 watched episode
- one movie search result known to be watched

## Reminder

If emulator and Shield disagree:
- trust the Shield for final behavior
- keep emulator as the fast feedback loop for layout and focus debugging
