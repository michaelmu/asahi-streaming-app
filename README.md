# shield-streaming-app

Android phone app for connecting to `codex app-server` over websocket.

## Status
Early Codex Android client prototype.

## Scope
This app is currently a phone-focused Android client, not an Android TV / Shield / Google TV app.

Current focus:
- connect to a local or reachable `codex app-server`
- list and resume threads
- send prompts
- stream assistant output
- handle approvals and user-input requests

## CI / APK artifacts
GitHub Actions is configured to build an unsigned **debug APK**.

Current workflow behavior:
- builds on `main`, `master`, pull requests, and manual dispatch
- uploads `shield-streaming-app-debug.apk` as an Actions artifact
- publishes a rolling prerelease tagged `latest-debug` on pushes to `main`/`master`

For GitHub-hosted builds this repo now expects the Gradle wrapper (`./gradlew`) checked in.

## Notes
Some workspace documents in the parent folder still describe a separate Shield/streaming-app direction. They are not the source of truth for this app.

Primary reference for this app:
- `../codex-android-client-spec.md`
