# Stremio Player Notes (APK Inspection)

## Context

The Stremio Android / Android TV app repository does not appear to be publicly available in the checked local workspace or easily discoverable as a public GitHub repo. To still learn from its player implementation, the APK was downloaded and inspected directly.

APK inspected:
- `com.stremio.one-2.1.5-4208893-arm64-v8a.apk`

---

## Verified Findings

### 1. Stremio uses Media3 / ExoPlayer internally

Evidence found in the APK:
- `libmedia3ext.so`
- `androidx/media3/...` references
- `exo_player_view`
- `exo_player_control_view`

Conclusion:
- Stremio is using **Media3 / ExoPlayer** for internal playback
- They are not using a completely separate custom playback stack

---

### 2. Stremio supports external player handoff

Strings and intent/deep-link references found in the APK:
- `Play with ExoPlayer`
- `Open in external player`
- `Play in external player`
- `Always start video in external player`
- integrations for players like VLC / Just Player / MPV / Infuse / MX-style intents

Conclusion:
- Stremio supports both:
  - internal ExoPlayer / Media3 playback
  - optional external player handoff

This is likely useful for compatibility edge cases.

---

### 3. Stremio models playback as runtime events/state

Player-related event strings found in the APK:
- `PlayerPlaying`
- `PlayerStopped`
- `PlayerEnded`
- `PlayerNextVideo`

Conclusion:
- Playback is treated as a real app/runtime session with meaningful state transitions
- This reinforces the need for Asahi to model playback as a durable session rather than just a screen destination

---

### 4. Stremio likely uses standard Exo/Media3 control resources as a base

Evidence:
- `exo_player_view`
- `exo_player_control_view`

Conclusion:
- Stremio appears to build on top of Media3 / ExoPlayer player views and control infrastructure
- Their player likely depends more on controller visibility / behavior / app integration than on a giant always-on custom overlay

---

## Implications for Asahi

### We should keep Media3

The problem with Asahi's player is not that Media3 is the wrong tool.
The problem is how we currently use it.

### Recommended direction

1. Move away from the always-on opaque custom player panel
2. Move toward transient player controls with video remaining primary
3. Improve playback state mapping (`Playing`, `Paused`, `Buffering`, `Ended`) so labels are trustworthy
4. Treat playback as a durable session with explicit transitions/events
5. Consider optional external-player support later for compatibility

---

## Practical Next Steps

### Near-term
- Remove or reduce the permanent custom overlay
- Lean more on transient Media3-style controls
- Keep app-specific metadata minimal and secondary
- Improve play/pause/buffer/ended mapping

### Later
- Add optional external player support
- Add `next episode` / `PlayerNextVideo` style flow
- Add audio/subtitle controls and richer transport behavior

---

## Bottom Line

The APK strongly suggests that Stremio's Android TV player approach is:

- Media3 / ExoPlayer underneath
- event/session-oriented playback logic around it
- optional external-player handoff
- less heavy always-on UI than the current Asahi player overlay

That is a good direction for Asahi to follow.
