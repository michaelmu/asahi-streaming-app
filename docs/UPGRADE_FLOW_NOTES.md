# Upgrade Flow Notes

> Historical notes plus current update-flow context.

## Problem

The update experience started weak and some of these notes describe the earlier state.

Current update work has already improved the flow substantially:
- in-app update checks exist
- in-app APK download/install handoff exists
- update gating now checks that the remote build number is higher
- Android TV install/update may still fail with `App not installed` if signing identity differs

## Most likely causes

### 1. Version code was not increasing
Previously the app always built with:
- `versionCode = 1`

This makes update-in-place unreliable or impossible once multiple builds exist.

### 2. Signing mismatch
If the currently installed APK and the downloaded APK are not signed with the same certificate, Android will reject the update.

This is a classic cause of:
- `App not installed`

### 3. Browser-driven install flow is weak on TV
Opening the APK URL externally gives us poor control over:
- download location
- install handoff
- user guidance
- failure diagnostics

---

## Fixes implemented so far

### Version code
The build now uses an increasing `versionCode`.

Priority order:
1. `ASAHI_VERSION_CODE` property / env var when provided
2. fallback to git commit count locally

### CI flow
GitHub Actions now sets:
- `ASAHI_VERSION_CODE = github.run_number`

The current pipeline builds signed release APKs, so CI-produced release artifacts should monotonically increase version code over time.

### Release notes
The rolling release body includes:
- `Version code: <run_number>`

This makes update diagnostics easier.

---

## Important limitation

Even with a correct increasing `versionCode`, updates will still fail if the installed APK and downloaded APK are not signed with the same key.

So if users install:
- one build signed locally/debug
- then try to update with a differently signed build

Android may still report:
- `App not installed`

---

## Recommended next upgrade-flow work

### 1. Ensure consistent signing for updateable builds
This is mandatory for a smooth update path.

### 2. Continue improving update diagnostics in UI
Show clearly:
- current versionName
- current versionCode
- latest versionName
- latest versionCode hint when available
- warning that signature mismatch will block upgrade

### 3. Optionally add signer preflight checks
A future improvement would be comparing the installed app signer and downloaded APK signer before launching install, so the app can explain signature mismatch directly instead of relying on Android's generic `App not installed` error.

---

## Bottom Line

The upgrade flow problem was not only a UI problem.
It was also a build/versioning problem.

Fixing versionCode progression is the first required step.
The next required step is consistent signing.
The best UX step after that is a real in-app installer flow.
