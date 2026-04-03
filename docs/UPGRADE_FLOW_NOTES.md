# Upgrade Flow Notes

## Problem

The current update experience is weak:
- the app only opens a browser/download link for the latest APK
- Android TV install/update may fail with `App not installed`

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
The GitHub Actions APK workflow now sets:
- `ASAHI_VERSION_CODE = github.run_number`

That means CI-produced debug APKs should now monotonically increase version code over time.

### Release notes
The rolling debug release body now includes:
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

### 2. Add in-app APK download + install flow
Instead of opening a browser URL:
- download APK in-app
- expose via `FileProvider`
- launch package installer directly

### 3. Improve update diagnostics in UI
Show:
- current versionName
- current versionCode
- latest versionName
- latest versionCode hint when available
- warning that signature mismatch will block upgrade

---

## Bottom Line

The upgrade flow problem was not only a UI problem.
It was also a build/versioning problem.

Fixing versionCode progression is the first required step.
The next required step is consistent signing.
The best UX step after that is a real in-app installer flow.
