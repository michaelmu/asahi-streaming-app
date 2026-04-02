# Media Player Notes

Last updated: 2026-04-02 UTC

## Purpose

Working notes for Asahi's playback stack: what is implemented, what we have validated, what is failing, and what lessons we should borrow from Kodi, Stremio, and Media3/ExoPlayer.

This document is intentionally practical. It is not a high-level product pitch; it is a running implementation/reference note for player behavior on Android TV / phones / emulator.

---

## Current Playback Stack

### Runtime path

Current Asahi playback flow is:
1. Search title
2. Load TMDb-backed metadata/details
3. Find sources via provider pipeline
4. Resolve source via Real-Debrid when needed
5. Build playback item
6. Hand stream URL to Media3 / ExoPlayer-backed playback engine
7. Render into `PlayerView` inside the app

### Current player implementation

Current player implementation is:
- Android-native
- Media3 / ExoPlayer based
- embedded `PlayerView`
- direct URL playback after RD resolution/unrestriction

### Current player diagnostics surfaced

We now expose/capture:
- player state label (`idle`, `buffering`, `ready`, `ended`, `stopped`, etc.)
- playback position / duration
- video format
- video size
- playback error
- current playback URL

These are available in the app playback panel and/or debug info copy path.

---

## What Is Confirmed Working

### Upstream pipeline

The following pieces are now confirmed working in current development state:
- Real-Debrid auth flow
- token persistence in Android app-private storage
- TMDb key embedding in Android builds
- live TMDb IDs on Android
- live Torrentio source fetching on Android
- cached Real-Debrid-aware source lists from Torrentio
- source resolution path reaching Media3 playback

This means playback debugging is now focused on the **actual player/render/decode layer**, not on fake data or broken auth.

### Concrete observed good state

In emulator validation after the TMDb fix, Asahi reached a state like:
- real TMDb id present
- IMDb id present
- Torrentio returning many live sources
- fallback count = 0

This is the baseline we want on real devices too.

---

## Known Current Playback Problems

### Symptom: audio + subtitles, but black video

Observed on-device:
- selected movie starts enough to produce audio
- subtitle track appears / behaves as expected
- video remains black

This is an important clue.

### What that likely means

This symptom strongly suggests that:
- source lookup is working
- RD resolution is working
- playback URL is at least partially valid
- the player can parse/open the stream enough to expose tracks
- the remaining problem is likely one of:
  - video codec/profile/level compatibility
  - HDR / Dolby Vision / HEVC rendering issue
  - MediaCodec / hardware acceleration quirk
  - surface/rendering path issue (`SurfaceView` vs `TextureView` style problem)
  - selected stream being too aggressive (large remux / high-bitrate / problematic profile)

### What it probably is *not*

Given audio + subtitles are present, this is less likely to be:
- broken TMDb metadata
- broken Torrentio fetching
- broken RD auth
- broken token persistence
- totally invalid stream URL

Those would usually fail earlier or more completely.

---

## Ranking / Source Selection Lessons

### Previous behavior

Earlier ranking over-favored very high-end results, especially:
- 4K
- huge files
- remux-heavy streams

That is not ideal for first-pass playback reliability.

### Current direction

We have started biasing toward safer choices:
- cached results first
- practical 1080p / 720p results before giant 4K remuxes
- penalties for oversized files
- penalties for remux-heavy releases

### Why this matters

For a v1 player, “most likely to play cleanly” is often better than “highest flex spec.”

Especially on Android TV / mixed Android hardware, safer defaults usually mean:
- fewer black-screen cases
- fewer hardware decoder edge cases
- better first impression

### Further ranking improvements worth considering

Potential additional ranking/filter preferences:
- prefer AVC / simpler HEVC before exotic codecs when reliability matters
- penalize Dolby Vision/HDR-heavy releases for default playback path if device behavior is uncertain
- penalize very high bitrate remuxes more strongly
- allow a user toggle later for:
  - quality-first
  - compatibility-first

---

## Lessons from Kodi

### Relevant pattern

Kodi's Android playback issues often involve:
- `MediaCodec`
- `MediaCodec (Surface)`
- hardware acceleration differences
- device-specific color/render/black-screen quirks

### Translation for Asahi

For Asahi, that suggests we should treat these as first-class debugging levers:
- rendering surface choice
- decoder/hardware acceleration path
- stream compatibility rather than just URL validity

### Practical takeaway

If we see black video with audio, we should consider:
- alternative surface rendering path
- hardware decode/render differences
- device-specific playback mode toggles

---

## Lessons from Stremio

### Relevant pattern

Stremio Android playback discussions/issues strongly suggest:
- black-screen-with-audio is a familiar class of problem
- giant HDR/DV/HEVC streams are often involved
- users sometimes solve issues by:
  - choosing smaller files
  - switching playback mode/backend
  - changing playback-related settings

### Translation for Asahi

For Asahi, that means:
- player mode should probably not be treated as fixed forever
- fallback playback strategies may be worth having
- source compatibility heuristics matter a lot

### Practical takeaway

A single playback path is fine for v1, but the architecture should leave room for:
- alternate player configuration modes
- possibly alternate player backends later
- explicit compatibility-focused source selection

---

## Media3 / ExoPlayer-Specific Notes

### Surface configuration matters

Android's Media3 docs explicitly treat video surface choice as a real configuration concern.

That means `PlayerView` defaults are not necessarily the end of the story.

Potential next experiment:
- test alternate rendering surface behavior
- e.g. try a `TextureView` path / equivalent supported player surface mode

### Why this matters

If a stream opens with:
- audio present
- subtitles present
- black video

then the issue may be:
- decoder output not rendering to the current surface
- or video track decode/render failing while audio remains fine

### Observability we should keep improving

Useful player telemetry for troubleshooting:
- current player state
- selected track formats
- video width/height
- video mime type / codec string if available
- playback exceptions from Media3
- whether first frame rendered

---

## Current Architecture Recommendations for Playback

### Keep playback behind a clean boundary

Even if v1 uses only Media3, playback should stay behind a separate subsystem boundary.

Reason:
- we may need alternate configs or fallback modes later
- source resolution and playback are distinct concerns
- UI should not own playback details

### Recommended playback concerns split

Suggested concerns:
- **player host**: Android view/surface ownership
- **playback engine**: Media3/ExoPlayer lifecycle + diagnostics
- **playback preparation**: map resolved stream into playable item
- **compatibility/ranking heuristics**: reduce bad stream selections early
- **player diagnostics**: expose useful runtime state to UI/debug tools

---

## Immediate Next Steps

### High priority

1. Keep validating black-screen cases with copied debug info
2. Improve player diagnostics further if current state is still insufficient
3. Experiment with alternate rendering/surface behavior
4. Continue biasing ranking toward safer/default-playable results

### Medium priority

1. Consider explicit playback mode toggle for debugging
   - standard/default
   - alternate surface/render mode
2. Add more detailed track/codec information into debug output
3. Test multiple known-good 1080p cached sources before assuming resolver failure

### Lower priority / later

1. Add optional external player fallback
2. Add explicit user-facing compatibility settings
3. Support more advanced per-device playback tuning

---

## Current Best Working Hypothesis

As of now, the strongest hypothesis for the black-video issue is:

> Asahi is successfully resolving and starting some real streams, but the selected stream or current rendering path is triggering a video decode/render compatibility problem on the device.

That means the next work should focus on:
- compatibility-aware source selection
- richer Media3 diagnostics
- alternate rendering mode experiments

not on re-debugging auth or provider plumbing.
