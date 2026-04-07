# Android Auto Emulator / DHU Setup

## Purpose

Set up a repeatable local Android Auto test environment for Asahi's media-app integration.

This complements the normal TV emulator notes in `docs/EMULATOR_TESTING.md`.

Use this when you want to validate:
- Android Auto media app discovery
- root browse nodes
- child browse flows (`Continue Watching`, `Favorites`, `Recent`, `Movies`, `TV Shows`)
- Auto search behavior
- search-result → playback handoff
- empty / failure states

## Recommended test shape

For now, the fastest honest validation loop is:
1. run the app on a phone emulator or compatible Android device
2. run Desktop Head Unit (DHU)
3. connect DHU over `adb`
4. validate browse / search / playback flows manually

This project is currently using a `MediaLibraryService`-based Android Auto companion surface, so DHU is the right first manual environment even before deeper car-specific polish.

## What to configure locally

Create or update `local.properties` with the Android SDK path:

```properties
sdk.dir=/opt/android-sdk
```

Optional: add the Android Auto helper values from `docs/local.android-auto.properties.example`.

## Example local.properties additions

These keys are optional convenience values for local scripts / manual copy-paste:

```properties
ANDROID_AUTO_SDK_DIR=/opt/android-sdk
ANDROID_AUTO_ADB=/opt/android-sdk/platform-tools/adb
ANDROID_AUTO_EMULATOR=/opt/android-sdk/emulator/emulator
ANDROID_AUTO_PHONE_AVD=Pixel_8_API_34
ANDROID_AUTO_DHU_MAIN=/opt/android-sdk/extras/google/auto/desktop-head-unit
```

Notes:
- paths may differ on your machine
- keep real local paths in `local.properties`, not committed docs
- if you do not have DHU installed under `extras/google/auto`, use Android Studio SDK Manager to install the Android Auto Desktop Head Unit package

## Baseline environment checks

List AVDs:

```bash
/opt/android-sdk/emulator/emulator -list-avds
```

Check `adb`:

```bash
/opt/android-sdk/platform-tools/adb devices
```

Check DHU install:

```bash
ls /opt/android-sdk/extras/google/auto/
```

## Start a phone emulator for Android Auto testing

Use a phone emulator, not the TV emulator, for Android Auto + DHU testing.

Example:

```bash
/opt/android-sdk/emulator/emulator \
  -avd Pixel_8_API_34 \
  -no-snapshot-save
```

For headless environments, try:

```bash
/opt/android-sdk/emulator/emulator \
  -avd Pixel_8_API_34 \
  -no-window \
  -no-snapshot-save \
  -no-boot-anim \
  -gpu swiftshader_indirect
```

Wait for boot:

```bash
export PATH=/opt/android-sdk/platform-tools:$PATH

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 5
done
adb devices
```

## Install the app build

From the repo root:

```bash
./gradlew :app:installDebug
```

If you are iterating on code first, a good fast sanity pass is:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## Enable Android Auto developer mode on the emulator/device

On the phone emulator/device:
1. open the Android Auto app
2. tap the version entry repeatedly to unlock developer settings
3. open the three-dot menu
4. enable **Start head unit server**

If the Android Auto app is missing, install/update the required Google system image / Play-enabled image first.

## Connect DHU

With the head unit server enabled:

```bash
adb forward tcp:5277 tcp:5277
```

Then launch DHU from the SDK install.

Common locations include one of these:

```bash
/opt/android-sdk/extras/google/auto/desktop-head-unit
/opt/android-sdk/extras/google/auto/desktop-head-unit.exe
```

If the binary is directly executable:

```bash
/opt/android-sdk/extras/google/auto/desktop-head-unit
```

If only the JAR/package wrapper is available on your install, launch it using the method provided by that SDK package.

## Manual validation checklist for Asahi Auto

Minimum useful pass:

### Discovery
- Asahi appears as a media app in DHU
- opening Asahi shows the root Auto surface

### Root browse
- `Continue Watching` shows content or an explicit empty state
- `Favorites` shows content or an explicit empty state
- `Recent` shows content or an explicit empty state
- `Movies` shows a real flat playable list
- `TV Shows` shows a real flat playable list
- `Search` is available

### Child browse behavior
- movie items are directly playable
- show items trigger default show playback behavior rather than deep browse trees
- empty branches do not silently fail

### Search
- movie search returns playable movie results
- show search returns playable show results
- empty query / empty result cases stay explicit and concise

### Playback handoff
- selecting a result reaches the playback selection path
- cached/direct-allowed items proceed
- blocked/no-safe-source cases fail cleanly instead of opening complex source picking flows

## Notes about the current MVP

The current Android Auto implementation is intentionally:
- playback-first
- cached/direct only
- deterministic for shows
- not a full TV app clone

So when validating manually, do **not** expect:
- manual source selection
- deep season browsing
- setup/auth repair flows in-car

## Troubleshooting

### Asahi does not appear in DHU
- confirm the debug build is installed
- confirm Android Auto developer mode is enabled
- confirm the head unit server is running
- confirm the app manifest still exposes the `MediaLibraryService`
- disconnect/reconnect DHU after reinstalling the app

### DHU cannot connect

Re-run:

```bash
adb devices
adb forward tcp:5277 tcp:5277
```

Then relaunch DHU.

### The emulator image does not support Android Auto well

Use a recent Play-enabled phone image if possible. TV images are not the right target for DHU-based Android Auto validation.

### Media app opens but content is missing

Check whether local stores actually contain favorites/history/continue-watching data. Some branches intentionally project existing signals rather than synthetic demo data.

### Search returns nothing

Validate TMDB/search configuration and network access in the emulator environment.

## Recommended local convention

Use one dedicated phone AVD for Android Auto validation, separate from the TV AVD.

Suggested name:

```text
asahi-auto-test
```

That keeps TV UI validation and Auto validation from stepping on each other.
