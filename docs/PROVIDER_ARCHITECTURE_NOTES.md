# Provider Architecture Notes

## Current assessment

Asahi already has the beginnings of a good multi-provider architecture:

- `SourceProvider`
- `RawProviderSource`
- `ProviderRegistry`
- `SourceRepositoryImpl`
- `SourceNormalizer`
- `SourceRanker`
- template-style provider pipeline (`queryBuilder` / `transport` / `parser`)

This is a solid foundation.

However, the current implementation is still partly prototype-oriented:

- `AppContainer` still registers fake/sample/demo providers alongside real ones
- `SourceRepositoryImpl` contains Torrentio-specific debug/summary logic
- provider capability/config metadata is not yet formalized
- provider composition is a simple list rather than a real selection/filtering layer

## Recommendation

Do a small cleanup pass now before porting more providers like Comet, MediaFusion, BitSearch, or Knaben.

### Do not rewrite from scratch

The current provider abstraction is worth keeping.

### Do refine it before scaling

Recommended cleanup goals:

1. Remove fake/sample providers from the default production registry
2. Make `ProviderRegistry` responsible for provider composition / filtering
3. Move provider-specific debug logic out of `SourceRepositoryImpl`
4. Add provider capability metadata (movies / episodes / packs / requires RD / etc.)
5. Keep Torrentio working unchanged while making room for additional providers

## Why this matters

Without cleanup, adding several more providers will work technically but make the codebase progressively messier:

- provider-specific branching will leak into generic layers
- debug behavior will stay coupled to Torrentio
- registry wiring will become a junk drawer
- it will be harder to add RD-only provider selection cleanly

## Suggested next architecture shape

### Provider interface remains the core abstraction

Keep `SourceProvider`, but consider enriching it with metadata such as:

- `id`
- `displayName`
- `kind`
- capabilities
- default enabled state
- optional priority / weight

### Provider capabilities

Add a provider capability model such as:

- supports movies
- supports episodes
- supports season packs
- supports series packs
- requires Real-Debrid
- returns magnets
- returns resolved links

This will help with filtering, ranking, UI display, and future provider toggles.

### Provider registry

Evolve `ProviderRegistry` from a plain list holder into a composition/filtering layer.

Potential responsibilities:

- expose active providers
- filter by app mode (RD-only, stable-only, debug/demo)
- centralize production vs experimental provider selection
- keep provider enable/disable behavior out of `AppContainer`

### Source repository

`SourceRepositoryImpl` should stay provider-agnostic.

It should only:

- call providers
- normalize results
- mark cached results
- rank results
- return shaped output

It should not contain Torrentio-specific counters, preview handling, or provider-id-specific metrics logic.

### Provider families

The codebase will likely benefit from recognizing provider families:

1. structured API providers
   - Torrentio
   - Comet
   - MediaFusion
   - YTSMX

2. HTML scrape providers
   - BitSearch
   - Knaben
   - TorrentGalaxy

3. fake / sample / experimental providers
   - should be kept separate from the default production registry path

## Practical implementation order

1. document architecture intent
2. cleanup current production registry
3. decouple repository from Torrentio-specific diagnostics
4. add provider capability metadata
5. then start porting new providers

## Current state after cleanup and first provider wave

Completed:
- production registry cleaned up to exclude fake/sample providers by default
- provider capability metadata added
- generic source flow decoupled from Torrentio-specific shaping
- additional validated providers added successfully using the same architecture:
  - Comet
  - BitSearch
  - Knaben

This is a good sign that the refined architecture is holding up under real provider expansion.

## Immediate recommendation

Before porting more providers:

- improve provider visibility in the UI
- add provider diagnostics/toggles in settings
- keep requiring smoke coverage plus live integration validation for new providers when feasible
- avoid adding providers that require extra provider-specific auth/config unless there is a strong product reason

That should keep the provider stack high-signal and maintainable.
