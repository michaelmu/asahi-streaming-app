# Player Implementation Checklist

This is the execution checklist companion to `PLAYER_IMPROVEMENTS_PLAN.md`.

---

## Phase 1 — Playback Foundation

### Playback session model
- [ ] Create `PlaybackSession` model
- [ ] Store current media/source/resolved stream separately from screen destination
- [ ] Persist enough session state to survive activity recreation
- [ ] Reattach player safely after rotation/recreate
- [ ] Ensure invalid player restores fall back gracefully

### Progress + resume
- [ ] Save playback progress periodically while playing
- [ ] Save on pause
- [ ] Save on stop/background
- [ ] Add watched-progress thresholds
- [ ] Add “Resume / Start Over” prompt
- [ ] Back continue-watching rail with real stored progress

### Restore hardening
- [ ] Validate `PLAYER` destination on restore
- [ ] If no valid source/session exists, recover to sources/details/episodes/results/home
- [ ] Preserve episode/movie context during restore
- [ ] Preserve last known playback position metadata

---

## Phase 2 — TV Player UX

### Core player chrome
- [ ] Replace debug-heavy overlay with real player chrome
- [ ] Show title + subtitle/episode context
- [ ] Show provider / quality / source badge
- [ ] Show progress bar + current/remaining time
- [ ] Keep debug info behind a secondary action

### Transport controls
- [ ] Add left/right seek behavior
- [ ] Add small and large seek intervals
- [ ] Handle play/pause key explicitly
- [ ] Improve center/OK behavior for overlay visibility
- [ ] Make remote interaction feel predictable on TV

### Track controls
- [ ] Surface available audio tracks
- [ ] Surface available subtitle tracks
- [ ] Add subtitle on/off toggle
- [ ] Add track picker UI
- [ ] Persist preferred subtitle/audio choices if needed later

### Secondary controls
- [ ] Add playback speed control
- [ ] Add any fit/zoom controls only if actually useful
- [ ] Review whether render-mode toggle should remain user-visible

---

## Phase 3 — Streaming Continuity

### Episode continuation
- [ ] Detect next episode from details state
- [ ] Offer “Next Episode” after playback ends
- [ ] Add “Replay” and “Back to Episodes” actions
- [ ] Optionally add autoplay-next countdown later

### Continue watching improvements
- [ ] Add real progress bars to continue-watching cards
- [ ] Show episode label / movie year context
- [ ] Show last played recency if useful
- [ ] Make continue-watching entries launch resume flow directly

### Error recovery
- [ ] Add playback failure overlay
- [ ] Add Retry action
- [ ] Add Back to Sources action
- [ ] Add Pick Another Source action
- [ ] Improve failure messaging by error type

---

## Engine / Media3 Work

### Playback engine
- [ ] Support prepare with optional start position
- [ ] Add explicit seek API
- [ ] Expose track groups / selected tracks
- [ ] Expose end-of-playback events
- [ ] Improve buffering/error event reporting

### Playback state
- [ ] Update current position on interval
- [ ] Report duration changes reliably
- [ ] Track ended state explicitly
- [ ] Surface current track metadata
- [ ] Surface more useful player capability flags

---

## Recommended Build Order

### First slice
- [ ] `PlaybackSession` model
- [ ] progress persistence
- [ ] resume/start-over prompt
- [ ] restore-safe player/session handling

### Second slice
- [ ] real TV player chrome
- [ ] seek shortcuts
- [ ] cleaner overlay behavior

### Third slice
- [ ] subtitle/audio picker
- [ ] failure recovery actions
- [ ] next-episode flow

---

## Definition of “Player Feels Real”

We should consider the player meaningfully upgraded when all of these are true:

- [ ] rotating the screen does not strand playback state
- [ ] user can resume from prior progress
- [ ] user can seek comfortably with a remote
- [ ] user can choose subtitles/audio tracks
- [ ] playback errors have a clear recovery path
- [ ] show playback naturally offers next-episode continuation
