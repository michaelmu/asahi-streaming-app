# Player Refactor Roadmap

This roadmap combines findings from:
- `PLAYER_IMPROVEMENTS_PLAN.md`
- `PLAYER_IMPLEMENTATION_CHECKLIST.md`
- `STREMIO_PLAYER_NOTES.md`
- `KODI_PLAYER_NOTES.md`

The goal is to move Asahi's player toward:
- **Stremio-like playback feel**
- **Kodi-like playback robustness**
- while staying grounded in **Media3 / ExoPlayer**

---

## Guiding Principles

### 1. Video is primary
The player UI should never feel like a dashboard sitting on top of the content.

### 2. Controls should be transient
UI appears when needed and gets out of the way when not needed.

### 3. Playback is a session, not just a screen
Playback state should survive recreation and drive resume/continuity logic.

### 4. Advanced controls belong in deeper layers
Audio/subtitles/options should exist, but not clutter the default playback view.

### 5. State labels must be trustworthy
Playing, Paused, Buffering, Ended, Idle, Error must map cleanly to real player state.

---

## Target Outcome

A finished Asahi player should behave like this:

- playback launches into a clean, video-first screen
- minimal info is shown only when useful
- standard transport controls are available through Media3/controller interaction
- resume works reliably
- progress updates drive continue-watching
- series playback supports next episode flow
- errors provide clear recovery paths
- subtitle/audio controls are accessible without cluttering the default player UI

---

## Phase 1 — Stabilize the Current Player Direction

### Status
Partially complete.

### Goals
- remove intrusive overlays
- improve state mapping
- keep video primary

### Tasks
- [x] Remove giant always-on custom player panel
- [x] Hide metadata during active playback
- [x] Improve state mapping (`playing`, `paused`, `buffering`, `ended`, `idle`)
- [ ] Verify Media3 controller visibility behavior on real devices
- [ ] Decide whether a tiny transient top bar is still needed

### Exit criteria
- player no longer obscures video during ordinary playback
- state labels are not obviously wrong
- playback feels less prototype-like

---

## Phase 2 — Make Playback a Real Session

### Status
Started.

### Goals
- durable playback continuity
- reliable resume behavior
- clearer lifecycle handling

### Tasks
- [x] Introduce persisted playback session store
- [x] Save progress/session metadata to disk
- [x] Hydrate continue-watching from stored playback session
- [x] Add initial resume matching logic
- [x] Add first resume prompt (`Resume / Start Over / Back to Sources`)
- [ ] Persist enough source/session identity to resume more confidently
- [ ] Reattach active playback session more cleanly after recreation
- [ ] Add stronger fallback behavior if resume session is invalid

### Exit criteria
- playback continuity survives recreation reliably
- resume prompt appears only when appropriate
- continue-watching reflects real stored progress

---

## Phase 3 — Build Real TV Playback Controls

### Goals
- rely on Media3 more naturally
- add app-specific controls only where they help

### Tasks
- [ ] Tune `PlayerView` / Media3 controller behavior
- [ ] Make controls visibility feel natural on TV remote input
- [ ] Decide whether to customize controller layout or keep close to Media3 defaults
- [ ] Add a minimal transient info layer only when controls are visible
- [ ] Ensure play/pause/seek interactions are intuitive with D-pad
- [ ] Validate controller behavior during playing/paused/buffering/ended states

### Notes
This phase should follow the Stremio direction:
- transient
- low clutter
- streaming-first

### Exit criteria
- player controls feel native to a TV app
- metadata appears only when invoked or useful
- transport behavior is predictable and clean

---

## Phase 4 — Resume / Progress / Continue Watching Polish

### Goals
- make playback continuity feel premium instead of approximate

### Tasks
- [ ] Save progress periodically while playing
- [ ] Save on pause/stop/background consistently
- [ ] Add watched thresholds
- [ ] Add proper progress bars and labels everywhere relevant
- [ ] Improve continue-watching card metadata (episode labels, recency, progress)
- [ ] Support direct resume from continue-watching entries
- [ ] Clear or reset resume once content is effectively completed

### Exit criteria
- resume/start-over logic feels dependable
- continue-watching is obviously useful and accurate

---

## Phase 5 — Tracks and Playback Options

### Goals
- make the player meaningfully usable beyond simple transport

### Tasks
- [ ] Add subtitle track picker
- [ ] Add audio track picker
- [ ] Add subtitle on/off toggle
- [ ] Expose track state through player/session model
- [ ] Decide whether playback speed belongs in the first options layer or later

### Notes
This is where Kodi’s layered-control model becomes more relevant.

### Exit criteria
- user can change audio/subtitles without leaving playback
- options do not clutter default playback UI

---

## Phase 6 — Failure Recovery and Stream Robustness

### Goals
- player failure should not dead-end the user

### Tasks
- [ ] Add playback failure overlay/actions
- [ ] Retry current source
- [ ] Back to sources
- [ ] Pick another source
- [ ] Better error classification and messaging
- [ ] Consider optional external-player fallback later

### Notes
Stremio APK inspection suggests external-player handoff is worth considering in the future.

### Exit criteria
- failures feel recoverable, not catastrophic
- user can recover without getting stranded

---

## Phase 7 — Episodic Flow / Next Episode

### Goals
- make TV series playback feel continuous and intentional

### Tasks
- [ ] Detect next episode from current details/session context
- [ ] Show next-episode action at end of episode
- [ ] Add replay action
- [ ] Add back-to-episodes action
- [ ] Consider autoplay-next countdown later

### Notes
This is where Kodi’s continuity mindset matters most.

### Exit criteria
- episode playback naturally leads into the next action
- player feels aware of series context

---

## Phase 8 — Optional External Player Support

### Goals
- improve compatibility for edge-case streams/devices

### Tasks
- [ ] Decide whether external player support is in scope
- [ ] Add settings toggle for external-player preference
- [ ] Add safe handoff path with metadata/URI support
- [ ] Keep internal player as default path

### Notes
This is inspired by Stremio’s APK behavior, not required for MVP.

### Exit criteria
- optional external-player support exists without complicating default UX

---

## Recommended Near-Term Build Order

If executed pragmatically, the best next sequence is:

1. **Phase 3** — real TV playback controls / Media3-first controller tuning
2. **Phase 4** — progress + continue-watching polish
3. **Phase 5** — subtitle/audio controls
4. **Phase 6** — failure recovery
5. **Phase 7** — next-episode flow

Reason:
- we already started the session/resume foundation
- the next highest user impact is the actual playback experience

---

## Success Definition

The player should feel “substantially done” when:

- [ ] normal playback leaves the video clean
- [ ] controls appear naturally and disappear naturally
- [ ] state labels are accurate
- [ ] resume works and feels trustworthy
- [ ] continue-watching is clearly useful
- [ ] subtitle/audio controls exist
- [ ] failures are recoverable
- [ ] episodic playback has next-step continuity

---

## Bottom Line

The right target for Asahi is:

- **Stremio for playback feel**
- **Kodi for playback structure**
- **Media3 as the actual implementation base**

That gives us the right balance of:
- simplicity
- robustness
- TV friendliness
- streaming practicality
