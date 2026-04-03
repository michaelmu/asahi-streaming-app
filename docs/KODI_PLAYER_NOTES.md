# Kodi Player Notes

## Context

Kodi is not the same kind of app as Asahi.
It is a full media-center platform, not just a streaming-first TV client.

That means Kodi's player should **not** be copied literally.
However, its playback architecture and interaction model contain several ideas that are useful for Asahi.

---

## Main Takeaway

Kodi's strength is not that its player looks minimal.
Its strength is that playback is treated as a **real stateful subsystem** rather than just a screen with a video URL.

Useful themes:
- explicit playback states
- layered controls
- better recovery behavior
- strong resume / watched / continuation logic
- separation between playback transport, info, and advanced options

---

## What Kodi Does Well

### 1. Playback is treated like a durable system

Kodi clearly separates:
- active playback session
- transport state
- player options
- resume/watch progress
- next-item behavior

This is important for Asahi because many of our player bugs come from treating playback mostly as UI state.

### 2. States are clearly differentiated

Kodi conceptually distinguishes between:
- playing
- paused
- buffering
- ended
- stopped/idle
- failure

That sounds obvious, but it matters.

Asahi should avoid vague or misleading labels such as showing a stopped-like state while playback is actually active.

### 3. Controls are layered

Kodi does not dump every option into a single flat overlay.
It tends to separate:
- basic transport controls
- playback info
- subtitles/audio/options
- post-playback actions

That is a better mental model for Asahi than one large permanent custom panel.

### 4. Resume and watched behavior are core features

Kodi is strong at:
- resume from prior position
- watched thresholds
- in-progress state
- clear completion logic

This reinforces the current Asahi direction toward:
- persisted playback session
- progress tracking
- resume prompt
- continue-watching rail

### 5. Playback recovery and continuity matter

Kodi handles the idea that playback is part of a longer flow.
Important concepts include:
- return to the right prior context
- next episode behavior
- replay
- recovery after failure

This is useful for Asahi, especially for TV-series flows.

---

## What Asahi Should Borrow from Kodi

### Borrow these ideas

#### Durable playback session model
Asahi should continue moving toward a playback session that survives recreation and maintains context.

#### Better state modeling
Asahi should keep explicit states such as:
- Playing
- Paused
- Buffering
- Ended
- Idle
- Error

#### Layered player UI
Asahi should separate:
- primary transport chrome
- secondary info
- advanced track/options UI
- failure / recovery UI

#### Resume / watched logic
Asahi should add:
- reliable resume
- watched thresholds
- clearer continue-watching behavior

#### Next-item flow
Asahi should support:
- next episode
- replay
- back to episode list

---

## What Asahi Should NOT Copy from Kodi

### 1. Too much UI density

Kodi can afford to be denser because it is a media-center environment.
Asahi should stay lighter and more streaming-focused.

### 2. Full media-center complexity

Asahi does not need to expose every control all the time.
It should preserve a cleaner TV-app feel.

### 3. Old-school “everything is a menu” interaction style

Asahi should keep quick, direct playback interactions where possible.

---

## Best Combined Direction

If Stremio and Kodi are both used as references, the best synthesis for Asahi is:

### From Stremio
- playback feel
- transient controls
- low-clutter playback screen
- streaming-first UX

### From Kodi
- session model
- state modeling
- resume/watched logic
- next-item flow
- recovery behavior

---

## Practical Direction for Asahi

### Short-term
- keep player UI minimal
- hide metadata during active playback
- improve Media3 state mapping
- avoid permanent opaque overlays

### Mid-term
- add subtitle/audio control layer
- add better pause/buffering/ended handling
- add next-episode actions
- add stronger recovery UI

### Long-term
- make playback a fully durable subsystem with real continuity and options

---

## Bottom Line

Kodi is useful for Asahi mostly as a reference for **player architecture and continuity**, not for exact visuals.

If Stremio is the reference for how the player should *feel*,
Kodi is the reference for how the player should *behave structurally*.
