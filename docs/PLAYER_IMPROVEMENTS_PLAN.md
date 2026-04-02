# Player Improvements Plan

## Why this exists

Asahi's current playback path works, but it is still closer to a prototype than a mature TV player. Compared against the player behavior expected from apps like **Stremio** and **Kodi**, the main gaps are not just visual — they are structural:

- playback is not modeled as a durable session
- resume/progress handling is thin
- recovery from errors is weak
- player controls are minimal
- audio/subtitle/track control is missing
- episodic continuation flow is missing

This document turns those observations into a concrete plan.

---

## Current State in Asahi

Current flow is roughly:

1. resolve source
2. build playback item
3. `Media3PlaybackEngine.prepare(...)`
4. attach `PlayerView`
5. show a minimal overlay/debug state

This is good enough to prove playback works, but it has important limitations:

- player state depends too heavily on UI state
- rotation/process recreation is brittle
- there is no real playback session persistence
- continue watching exists only as a lightweight home rail, not as a full resume system
- default player overlay is too debug-heavy and not TV-native enough

---

## Comparison Summary

### What Stremio does well

Stremio's Android TV player tends to emphasize:

- simple, low-clutter TV-first playback chrome
- continuity between browsing and playback
- stronger “continue / next thing” behavior
- practical player UI rather than debug/UI scaffolding
- straightforward streaming-focused UX

### What Kodi does well

Kodi's player is stronger on the “full media center” side:

- robust resume behavior
- watched / in-progress state
- audio/subtitle controls
- durable playback continuity
- next-episode / post-playback flows
- better recovery mindset when playback or stream selection goes wrong

### What Asahi should copy

From **Stremio**:

- simpler playback chrome
- stronger continuation flow
- less on-screen clutter
- tighter player-to-browse experience

From **Kodi**:

- resume tracking and prompts
- watched threshold logic
- subtitle/audio controls
- stronger error recovery
- episodic continuation support

---

## Core Problems to Solve

### 1. Playback is not a durable session

Right now playback is mostly represented indirectly through UI state.

That creates problems for:

- rotation
- activity recreation
- resume after interruption
- post-playback continuation

### 2. Resume handling is incomplete

We have the beginnings of a continue-watching rail, but not a full resume system.

Missing pieces:

- periodic position saving
- resume prompts
- watched thresholds
- completion rules

### 3. Player controls are too thin

Missing:

- audio track selection
- subtitle selection
- subtitle on/off
- playback speed
- seek shortcuts
- richer bottom-player controls

### 4. Error handling is too fragile

Playback failures should not strand the user.

Missing:

- retry playback
- return to sources
- quick source switch
- better human-readable error states

### 5. Episodic flow is incomplete

Missing:

- next episode prompt
- autoplay-next logic
- end-of-episode actions
- better handoff from player back to episodes

---

## Recommended Implementation Order

## Phase 1 — Playback Foundation

### 1. Introduce a durable Playback Session model

Create a dedicated model for active playback, separate from screen routing.

Suggested contents:

- media reference
- title / subtitle / episode context
- selected source metadata
- resolved playback URL
- headers / mime type
- playback start timestamp
- last known position
- duration
- watched state / completion state

Why first:

- fixes classes of restore/rotation bugs
- makes resume and progress tracking possible
- gives player UI a stable data source

### 2. Persist progress periodically

Add progress snapshots while playback is active.

Suggested behavior:

- save position every 10–15 seconds while playing
- save on pause
- save on stop
- save on background / destroy

Suggested metadata:

- media id / query fallback
- positionMs
- durationMs
- percent watched
- last played at

### 3. Add resume logic and watched thresholds

Suggested rules:

- if progress < 3% → treat as not started
- if progress between ~3% and 92% → show resume
- if progress > ~92% → mark watched / reset resume

Suggested UX:

- Resume from 23:14
- Start over

### 4. Harden restore / rotation behavior

On restore, player should never land on an invalid PLAYER screen.

Rules:

- if playback session exists and engine can reattach, reopen player cleanly
- if session is partial but playable, recover to source/details/episodes screen
- if broken, fail safely to a valid previous context

---

## Phase 2 — TV Player UX

### 5. Replace debug-heavy overlay with real TV player chrome

Default player overlay should show:

- title
- season/episode label when relevant
- provider / quality / source badge
- progress bar
- play/pause state
- current time / remaining time

Avoid making debug text the primary player UI.

Debug details can live behind:

- a long-press
- a developer toggle
- a hidden action in Settings

### 6. Add proper seek and transport controls

Suggested TV behavior:

- left/right = seek small interval
- long press / repeated press = larger jumps
- play/pause key support
- center/OK toggles overlay or pauses depending on mode

Suggested defaults:

- small seek: 10 seconds
- large seek: 30 seconds

### 7. Add audio/subtitle controls via Media3 tracks

Expose:

- available audio tracks
- available subtitle tracks
- subtitles on/off
- preferred/default track selection

This is one of the most obvious feature gaps compared to real media apps.

### 8. Add playback speed and scaling options later

Lower priority than audio/subtitle, but useful:

- playback speed selection
- zoom / fit / stretch behavior if needed
- renderer toggles only if still relevant after stabilization

---

## Phase 3 — Streaming App Continuity

### 9. Add next-episode flow

For shows:

- detect next episode from details/episode state
- offer next episode prompt near end or after completion
- optionally add autoplay-next countdown later

Suggested actions:

- Next Episode
- Replay
- Back to Episodes

### 10. Improve continue watching into full resume surface

Home rail should eventually be backed by actual playback history, not just lightweight UI state.

It should support:

- artwork
- last watched time
- progress bar
- episode label
- direct resume action

### 11. Better playback error recovery

On failure, offer:

- Retry
- Back to Sources
- Pick Another Source
- Back to Details / Episodes

Also distinguish between:

- resolve failure
- network failure
- unsupported stream
- player init failure
- stream ended / dead link

---

## Media3 / Engine Improvements

## Current engine limitations

Current `Media3PlaybackEngine` is intentionally simple but underpowered.

Key missing capabilities:

- richer session persistence
- track management APIs surfaced to UI
- better listener coverage
- better current-position update cadence
- media metadata usage
- resumed playback seeking
- richer playback event handling

## Recommended engine enhancements

### Engine responsibilities to add

- prepare with optional start position
- seekTo(positionMs)
- expose track groups / current selection
- expose play/pause/seek commands cleanly
- expose transport availability
- expose end-of-playback events
- expose buffering/recovery events more clearly

### State observation improvements

Current playback state flow is useful but incomplete.

It should include or support:

- current position updates on interval
- duration updates
- ended state
- seek in progress / buffering after seek
- selected audio/subtitle state
- track availability

---

## Suggested New Types

These are not final, but they show the direction.

### `PlaybackSession`

- sessionId
- mediaRef
- sourceResult
- resolvedStream
- displayTitle
- displaySubtitle
- artworkUrl
- seasonNumber
- episodeNumber
- lastPositionMs
- durationMs
- watchedPercent
- startedAt
- updatedAt

### `PlaybackProgressRecord`

- media key / id
- positionMs
- durationMs
- percentWatched
- lastPlayedAt
- lastSourceLabel

### `PlayerControlsState`

- overlayVisible
- canSeek
- canPause
- canChangeAudio
- canChangeSubtitles
- selectedAudioTrack
- selectedSubtitleTrack
- availableAudioTracks
- availableSubtitleTracks

---

## Best Next Step

If we only do **one thing next**, it should be:

## Build a durable playback session + resume/progress system

Why:

- it fixes restore/rotation issues at the root
- it unlocks real continue watching
- it enables resume prompts
- it makes next-episode and player chrome far easier to implement correctly

After that, the next best step is:

## Replace the current player overlay with proper TV playback chrome

---

## Short Action Checklist

### Immediate

- [ ] Create `PlaybackSession` model
- [ ] Persist current playback session separately from UI destination
- [ ] Save playback progress periodically
- [ ] Add resume/start-over prompt
- [ ] Make continue watching use real stored progress

### Near-term

- [ ] Replace debug overlay with TV player chrome
- [ ] Add seek shortcuts and transport UX
- [ ] Add subtitle/audio track picker
- [ ] Add playback failure recovery actions

### Later

- [ ] Add next-episode flow
- [ ] Add autoplay-next countdown
- [ ] Add watched/in-progress rules everywhere in browse UI
- [ ] Add richer playback settings

---

## Bottom Line

Asahi's player works, but compared to Stremio and Kodi it still lacks the structural backbone of a real TV playback system.

The biggest improvement is not cosmetic.
It is making playback a **durable, restorable session with real progress tracking**.

Everything else gets easier once that exists.
