# App Shell Status — 2026-04-04

This note records where the app shell ended up after the 2026-04-04 cleanup pass.

## What changed in this pass

The original shell review identified `MainActivity` as the main orchestration gravity well.
That pass is now largely complete.

During the cleanup, the following focused coordinators were introduced or expanded so `MainActivity` no longer owns their full workflow logic inline:

- `PlaybackLaunchCoordinator`
- `BrowseFlowCoordinator`
- `DetailsNavigationCoordinator`
- `BackNavigationCoordinator`
- `SourceLoadUiCoordinator`
- `SettingsCoordinator`
- `SettingsModalCoordinator`
- `SourcePreferencesCoordinator` usage was completed instead of bypassed

## What improved

`MainActivity` is still the shell host, but it is no longer carrying the same degree of workflow concentration.
Most of the highest-value seams from the original review were extracted:

- playback launch and playback-session preparation
- browse search/detail loading
- details/episodes/sources transition policy
- back-navigation policy
- source-loading UI lifecycle/progress presentation
- settings summaries and action entry points
- source-preferences modal composition
- Real-Debrid/update modal branching

This means future changes are less likely to collide in one giant activity method block.

## Remaining rough edges

These are the main remaining things worth watching, in rough priority order:

### 1. `MainActivity` is still the UI host for many side effects
It still owns:
- shell rendering
- modal display plumbing
- fullscreen/player attachment behavior
- auth QR rendering
- clipboard/debug dump behavior
- APK install intent launching

That is acceptable for now, but it means the file is still important even if it is no longer the whole app.

### 2. State restoration is still pragmatic rather than deeply modeled
`AppState` remains broad and is not yet a deeply structured workflow-state model.
That is fine, but edge-case restore behavior is still a likely future bug source.

### 3. Generic modal helper plumbing is still centralized in the activity
Most settings-specific modal branching moved out, but generic modal presentation still lives in `MainActivity`.
That is now a small cleanup opportunity, not a major architecture problem.

### 4. The app still uses explicit state transitions, not a richer navigation/history stack
Back behavior is now centralized, but navigation is still policy-driven rather than stack-driven.
That is a reasonable choice unless the app starts needing much more complex navigation semantics.

### 5. There is still an Android manifest warning during build
The remaining visible build warning at the end of this pass is the FileProvider manifest replacement warning, not the old source-ranking Elvis warning.

## Recommended next steps

If another shell cleanup pass is wanted later, the best order is probably:
1. review state restoration / resume reconciliation edge cases
2. optionally thin generic modal helper plumbing
3. only then consider whether `MainActivity` needs any further structural split

## Bottom line

The original architectural concern was real.
It is also no longer the repo’s dominant problem.

The shell is now in a much healthier state, and the next issues to solve are more likely to be product behavior, edge cases, or incremental cleanup rather than one giant orchestration file.
