# UI Refactor Plan

Last updated: 2026-04-02 UTC

## Goal

Refactor Asahi from the current single giant scroll-based debug/prototype page into a page-oriented TV-friendly application flow.

The current one-screen implementation was the right way to validate the vertical slice quickly, but it is no longer the right shape for continued product development.

This plan defines the target screen structure, navigation model, phased implementation path, and short-term priorities.

---

## Why Refactor Now

Asahi has now validated the core playback path:
- TMDb-backed metadata
- Torrentio live source fetching
- Real-Debrid auth
- Real-Debrid-backed source resolution
- in-app playback with Media3/ExoPlayer

That means the UI can now stop acting like a single debugging harness.

### Current UI problems

The current `MainActivity` is trying to be all of these at once:
- auth/settings screen
- search screen
- result list
- details page
- season/episode picker
- source selection page
- playback panel
- debug surface

This causes several problems:
- poor TV navigation structure
- too much vertical scrolling
- weak back-stack behavior
- hard to reason about state ownership
- hard to extend without making the activity worse
- playback is embedded inline instead of taking over the screen

---

## Target UX Structure

## Top-level flow

### Home
Primary landing page.

Responsibilities:
- top-level navigation
- choose content mode
- later: continue watching / recent / settings

Initial entries:
- Movies
- TV Shows
- Settings / Accounts

### Search
A focused search page.

Responsibilities:
- query entry
- mode-aware labels (Movies or TV Shows)
- trigger search

Possible design:
- shared search page with mode parameter
- or separate movie / TV search entry routes

Recommended initial approach:
- one search screen with a required `SearchMode`
- `MOVIES`
- `SHOWS`

### Results
Search results page.

Responsibilities:
- display result list or grid
- allow result selection
- keep search input separate from results presentation

### Details
Title details page.

Responsibilities:
- show title metadata
- overview
- artwork later
- entry point to the next step

For movies:
- action: `Find Sources`

For shows:
- action: `Browse Episodes`

### Season / Episode Picker
TV-only page.

Responsibilities:
- season selection
- episode selection
- episode summary list

Selecting episode leads to source selection.

### Source Selection
Dedicated page for source choice.

Responsibilities:
- display ranked sources
- show quality / cache / size / provider
- let user choose source
- optionally provide compatibility hints later

This page should stop looking like a debug panel and start looking like a chooser.

### Full-Screen Player
Player should own the full screen when active.

Responsibilities:
- render video full-screen
- controls overlay on top
- clean back behavior
- minimal but clear playback state

Back behavior should return to:
- source selection page
- or details page depending on desired UX

---

## Navigation Model

## Movie flow

Home  
→ Movie Search  
→ Results  
→ Movie Details  
→ Source Selection  
→ Full-Screen Player

## TV flow

Home  
→ TV Search  
→ Results  
→ Show Details  
→ Season / Episode Picker  
→ Source Selection  
→ Full-Screen Player

---

## Auth / Settings Placement

Real-Debrid auth should not live permanently in the middle of the content browsing flow.

### Recommended placement

Move it under:
- Settings
- Accounts
- or a dedicated Real-Debrid settings page

### Desired behavior

- browsing/search should work without auth
- selecting an RD-backed action should prompt appropriately if auth is required
- debug/auth tools should remain available, but not dominate the main UI

### Short-term compromise

Until the settings screen exists:
- auth controls can remain accessible from the home screen
- or behind a small status/action tile

---

## Recommended Technical Structure

## Short-term architecture

Do not immediately jump into a large Android Navigation/Fragment rewrite unless it is necessary.

Instead:
- keep one activity for now
- introduce explicit screen/page state
- render one page at a time
- remove the giant stacked scroll layout

### Example page state model

```kotlin
sealed interface AppScreen {
    data object Home : AppScreen
    data class Search(val mode: SearchMode) : AppScreen
    data class Results(val mode: SearchMode, val query: String) : AppScreen
    data class Details(val mediaRef: MediaRef) : AppScreen
    data class EpisodePicker(val title: TitleDetails) : AppScreen
    data class Sources(
        val mediaRef: MediaRef,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null
    ) : AppScreen
    data class Player(val source: SourceResult) : AppScreen
    data object Settings : AppScreen
}
```

This gives us a real navigation shape without forcing a huge framework move immediately.

---

## Component Split Recommendation

As part of the refactor, break `MainActivity` page rendering into dedicated classes/components.

Suggested first split:
- `HomeScreenRenderer`
- `SearchScreenRenderer`
- `ResultsScreenRenderer`
- `DetailsScreenRenderer`
- `EpisodePickerScreenRenderer`
- `SourcesScreenRenderer`
- `PlayerScreenRenderer`
- `SettingsScreenRenderer`

The activity should gradually become:
- app state holder
- navigation coordinator
- lifecycle owner

instead of the place where every UI widget is directly built inline.

---

## Phased Refactor Plan

## Phase 1 — Page-oriented structure without changing core data flow

Goal:
- preserve working playback stack
- replace single giant page with distinct screens

Tasks:
1. Introduce `AppScreen` sealed model
2. Replace giant stacked layout with one active page at a time
3. Add Home page
4. Split current search/results/details/source flow into separate pages
5. Keep player page full-screen or near-full-screen

Deliverable:
- same data functionality
- much cleaner navigation
- no more all-in-one page

## Phase 2 — Move playback to full-screen experience

Goal:
- remove inline playback panel
- make playback feel like a real player

Tasks:
1. Create dedicated player page/overlay
2. Ensure clean return/back behavior
3. Keep diagnostics accessible in debug builds
4. Keep playback controls visible but minimal

Deliverable:
- proper player experience

## Phase 3 — Move auth/debug/settings out of main flow

Goal:
- stop blending account/debug controls into content flow

Tasks:
1. Create settings/accounts page
2. Move RD auth there
3. Keep a small account status indicator elsewhere if needed
4. Retain `Copy Debug Info` in settings/debug area

Deliverable:
- main flow is content-first, not debug-first

## Phase 4 — TV polish

Goal:
- improve focus behavior and TV ergonomics

Tasks:
1. improve focus order
2. use more TV-appropriate page layouts
3. refine season/episode list behavior
4. refine source selection presentation

Deliverable:
- stronger Android TV / Shield UX

---

## Immediate First Refactor Slice

Recommended first implementation slice:

### Step 1
Create the following screens first:
- Home
- Search
- Results
- Details
- Sources
- Player

### Step 2
Keep season/episode selection initially attached to details or as a simple intermediate page.

### Step 3
Make the player full-screen as early as possible.

### Why this slice first

This gives the biggest UX improvement with the least risk because:
- search/results/details/source selection are already conceptually present
- the underlying use cases already work
- player isolation is a major UX win
- it avoids rewriting every sub-flow at once

---

## Screen-Specific Notes

## Home page

Should be very simple initially:
- Movies
- TV Shows
- Real-Debrid / Settings

Optional later:
- Recently Played
- Continue Watching
- Resume playback

## Search page

Should focus on search input only.

Potential layout:
- page title
- search field
- search action
- maybe recent searches later

## Results page

Should not also be details/source/player.

Potential layout:
- top query label
- result count
- simple vertical list first
- grid later

## Details page

Should include:
- title
- year
- type
- genres/runtime if available
- overview
- primary action button

## Sources page

Suggested default presentation:
- source name
- quality
- cache status
- size
- provider name
- maybe a compatibility hint later

Keep detailed raw diagnostics off the main row by default.
Use debug copy path instead.

## Player page

Should:
- own the whole screen
- prioritize video
- not sit in a scroll container
- keep minimal overlay controls

---

## Debug Strategy During Refactor

We should not lose our current debugging power while cleaning up UX.

Keep these capabilities somewhere in debug-friendly builds:
- version + git SHA
- `tmdb_key_embedded`
- auth state
- token path if needed
- playback state / error / video format / size
- copy debug info action

But move them out of the main content flow over time.

---

## Success Criteria

This refactor is successful when:
- there is no single giant content/debug page anymore
- the user can move through content in page-based steps
- the player takes over the screen when playback starts
- auth/debug/settings no longer dominate the browsing UI
- `MainActivity` is meaningfully smaller and less entangled

---

## Recommendation

Proceed with:
1. document the target structure (this document)
2. implement `AppScreen` and one-page-at-a-time rendering
3. split the first pages:
   - Home
   - Search
   - Results
   - Details
   - Sources
4. make player full-screen early
5. move auth/settings/debug to their own area shortly after

That gives the best balance of:
- keeping momentum
- improving UX quickly
- not destabilizing the working playback pipeline
