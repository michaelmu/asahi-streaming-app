# Release Signing Setup

## Goal

Configure a stable signing identity for production-ready APK updates.

## Generate a keystore

```bash
keytool -genkeypair \
  -v \
  -keystore asahi-release.keystore \
  -alias asahi \
  -keyalg RSA \
  -keysize 4096 \
  -validity 3650
```

Move the file somewhere outside the repo, or into a gitignored path such as:

```text
keystores/asahi-release.keystore
```

## Local configuration

Add these to `local.properties`:

```properties
ASAHI_RELEASE_STORE_FILE=keystores/asahi-release.keystore
ASAHI_RELEASE_STORE_PASSWORD=your-store-password
ASAHI_RELEASE_KEY_ALIAS=asahi
ASAHI_RELEASE_KEY_PASSWORD=your-key-password
```

You can also provide them via environment variables instead.

## Build a signed release APK locally

```bash
./gradlew assembleRelease
```

If signing values are present, the release build will use the configured release keystore.
If they are absent, Gradle will still build, but the release variant will not have the stable signing identity needed for upgrade-safe distribution.

## Important migration note

If devices already have a debug-signed build installed, Android will not accept an update signed with a different release keystore.

That means the first migration usually requires:
1. uninstall old debug-signed build
2. install the new release-signed build once
3. future in-app updates will work normally as long as signing + package name stay the same

## GitHub Actions / CI

GitHub Actions is already wired to build a signed release APK using repo secrets.

Required secrets:
- `ASAHI_RELEASE_KEYSTORE_B64`
- `ASAHI_RELEASE_STORE_PASSWORD`
- `ASAHI_RELEASE_KEY_ALIAS`
- `ASAHI_RELEASE_KEY_PASSWORD`

Workflow behavior:
- decodes the keystore during CI
- points `ASAHI_RELEASE_STORE_FILE` at the decoded file
- builds `assembleRelease`
- publishes the signed release artifact

If CI signing fails, first verify that `ASAHI_RELEASE_KEYSTORE_B64` is a clean single-line base64 string with no extra characters or wrapping.
