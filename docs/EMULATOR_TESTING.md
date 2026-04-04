# Emulator / Instrumentation Testing

## Purpose

Run Android instrumentation tests for Asahi on a local emulator or connected device.

Related docs:
- `docs/RELEASE_SIGNING_SETUP.md`
- `docs/PROVIDER_RESEARCH.md`
- `docs/PROVIDER_ARCHITECTURE_NOTES.md`

## Environment used successfully here

On this machine, the Android SDK is installed at:

```text
/opt/android-sdk
```

Useful binaries:

```text
/opt/android-sdk/emulator/emulator
/opt/android-sdk/platform-tools/adb
```

Known AVD discovered locally:

```text
asahi-tv-test
```

AVD storage observed here:

```text
/home/node/.android/avd
```

## Quick start for this environment

### 1. Start the emulator headlessly

This environment does **not** have a normal X display, so the emulator must be started with `-no-window`.

```bash
/opt/android-sdk/emulator/emulator \
  -avd asahi-tv-test \
  -no-window \
  -no-snapshot-save \
  -no-boot-anim \
  -gpu swiftshader_indirect
```

### 2. Wait for boot

```bash
export PATH=/opt/android-sdk/platform-tools:$PATH

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 5
done
adb devices
```

### 3. Run connected instrumentation tests

From the repo root:

```bash
./gradlew app:connectedDebugAndroidTest
```

## General requirements

You need a working Android SDK environment on the machine where you run these commands.

Helpful tools on `PATH`:
- `emulator`
- `adb`
- `avdmanager` (optional but useful)

If the tools are not on `PATH`, use full paths under `/opt/android-sdk/...` as shown above.

## List available AVDs

If `emulator` is on `PATH`:

```bash
emulator -list-avds
```

Or using the full path in this environment:

```bash
/opt/android-sdk/emulator/emulator -list-avds
```

## Alternative emulator start commands

Foreground:

```bash
emulator -avd <YOUR_AVD_NAME>
```

Background:

```bash
emulator -avd <YOUR_AVD_NAME> &
```

For headless environments, prefer:

```bash
emulator -avd <YOUR_AVD_NAME> -no-window -no-boot-anim -gpu swiftshader_indirect
```

## Run app instrumentation tests

From the repo root:

```bash
./gradlew app:connectedDebugAndroidTest
```

You can also run all connected checks:

```bash
./gradlew app:connectedCheck
```

## Useful related tasks

Install test APK only:

```bash
./gradlew app:installDebugAndroidTest
```

Uninstall test APK:

```bash
./gradlew app:uninstallDebugAndroidTest
```

## Local JVM sanity checks

For faster iteration between device runs:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Troubleshooting

### `adb` / `emulator` not found

Use full paths:

```bash
/opt/android-sdk/platform-tools/adb
/opt/android-sdk/emulator/emulator
```

Or export them onto `PATH`:

```bash
export PATH=/opt/android-sdk/platform-tools:/opt/android-sdk/emulator:$PATH
```

### Emulator fails with Qt / xcb display errors

If you see errors like:
- `could not connect to display`
- `Could not load the Qt platform plugin "xcb"`

Use headless mode:

```bash
-no-window
```

### Emulator appears connected but tests take a while

That can be normal.

A typical flow is:
1. emulator boots
2. `adb wait-for-device`
3. `sys.boot_completed` becomes `1`
4. Gradle builds debug + androidTest APKs
5. Gradle installs the APKs
6. `:app:connectedDebugAndroidTest` runs

The build/install phase can take longer than expected even after the device shows up in `adb devices`.

### Kotlin daemon / incremental compiler weirdness during connected tests

If Gradle throws odd Kotlin daemon or incremental cache errors, try:

```bash
./gradlew --stop
./gradlew clean
./gradlew app:connectedDebugAndroidTest
```

You can also rerun after a daemon fallback; in one observed run, Gradle recovered after the daemon issue and continued.

### Audio warnings from the emulator

Warnings like Pulseaudio init failures are usually not fatal for headless test runs.

## Recommended workflow

1. Start emulator headlessly
2. Confirm `adb devices` shows it
3. Wait until `sys.boot_completed=1`
4. Run `./gradlew app:connectedDebugAndroidTest`
5. Use JVM checks (`testDebugUnitTest assembleDebug`) for faster iteration between device runs

## Last verified notes

Observed in this environment:
- the `asahi-tv-test` AVD exists
- headless launch works with `-no-window`
- `adb` can see the emulator once booted
- `app:connectedDebugAndroidTest` can be launched from this environment once the SDK tools are referenced correctly
