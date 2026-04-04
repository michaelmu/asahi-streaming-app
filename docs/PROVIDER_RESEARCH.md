# Provider Research Notes

Related docs:
- `docs/PROVIDER_ARCHITECTURE_NOTES.md`
- `docs/EMULATOR_TESTING.md`

## Goal

Identify which local `cocoscrapers` providers are worth porting into Asahi.

Constraint for this pass:
- **Real-Debrid only**
- prioritize sources that either:
  - are already debrid-oriented / structured APIs, or
  - produce useful torrent magnets/hashes that Real-Debrid can ingest reliably

## Local source set reviewed

From the local cocoscrapers checkout, the following torrent providers were identified:

- torrentio
- comet
- mediafusion
- bitsearch
- knaben
- torrentgalaxy
- ytsmx
- eztv
- 1337x
- piratebay
- nyaa
- yourbittorrent
- torrentdownload
- torrentquest
- kickass2
- bitcq
- torrentproject2
- bitlord
- isohunt2
- torrentfunk

## Current validated status in Asahi

Implemented and validated locally:
- Torrentio
- Comet
- BitSearch
- Knaben

Notes:
- Comet has smoke coverage plus env-gated live integration coverage.
- BitSearch has smoke coverage plus live integration coverage and required parser adjustments against the current live site markup.
- Knaben has smoke coverage plus live integration coverage.
- MediaFusion was explored but deprioritized because it appears to require extra provider-specific `encoded_user_data` configuration beyond the desired low-friction Real-Debrid flow.
- YTSMX was explored but abandoned for now because the upstream host was not resolvable from the current environment, so it did not meet the smoke + live validation bar.

## Recommended RD-first ranking

### 1. Comet

Why:
- explicitly debrid-oriented
- structured/API-like integration rather than brittle HTML scrape-first behavior
- supports movies and episodes
- strongest fit for Asahi's current Real-Debrid-centric direction

Recommendation:
- highest priority port candidate

### 2. BitSearch

Why:
- broad torrent coverage
- useful fallback when debrid-native aggregators miss
- workable for RD because it yields magnets/hashes that RD can ingest

Caveat:
- HTML scraping means higher brittleness than Comet/MediaFusion

Recommendation:
- strong secondary source after structured RD-first providers

### 3. Knaben

Why:
- broad torrent meta-index behavior
- good fallback source for RD workflows
- implementation appears relatively straightforward

Caveat:
- still HTML scrape territory

Recommendation:
- worth porting after BitSearch

### 4. YTSMX

Why:
- easy movie-only specialist
- API-style access
- clean, high-confidence movie results for popular titles

Caveat:
- movies only
- narrower content coverage than general indexes

Recommendation:
- useful specialist, but not part of the main multi-content backbone

### 5. TorrentGalaxy

Why:
- historically high-value torrent source
- often good quality / seed counts

Caveat:
- more anti-bot / Cloudflare / mirror churn pain
- likely higher maintenance burden than the sources above

Recommendation:
- optional later addition, not first-wave

## Lower-priority / generally defer

These may still work with Real-Debrid in principle because they expose magnets/hashes, but they appear lower-value relative to effort/maintenance:

- 1337x
- piratebay
- eztv
- nyaa (unless anime becomes an explicit priority)
- yourbittorrent

## Probably skip for now

These appear more like brittle mirror roulette / long-tail maintenance burden than worthwhile first-wave ports:

- kickass2
- torrentproject2
- torrentdownload
- torrentquest
- bitcq
- bitlord
- isohunt2
- torrentfunk

## Recommended port order for Asahi

If staying focused on Real-Debrid-compatible value with minimal extra provider-specific setup:

1. Comet ✅
2. BitSearch ✅
3. Knaben ✅
4. YTSMX (deferred)
5. TorrentGalaxy (optional later)

Deprioritized:
- MediaFusion (requires extra provider-specific auth/config)

## Strategic recommendation

Avoid porting the entire cocoscrapers provider zoo.

A lean high-signal Asahi provider stack is likely better:

- Torrentio
- Comet
- BitSearch
- Knaben
- maybe YTSMX later if the upstream becomes reliably reachable

This gives:
- strong RD-friendly structured sources
- a couple of broad fallback torrent indexes
- lower maintenance burden than owning many brittle public scrapers
