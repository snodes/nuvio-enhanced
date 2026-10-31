# backdrop-max

## Changed from 9e64d6c5 (text-subs-first)

- New artwork-only TMDB `/images` picker uses the existing profile API-key setting even with TMDB enrichment OFF; no settings, season data, posters, titles, or episode metadata are rewritten.
- Largest textless (`null`/`xx`) backdrop by pixel area wins, then vote count; requests and serves `/t/p/original/`. Bounded in-memory cache (256 IDs, partitioned by media type/API key); missing key, lookup/network/parse failure, timeout, or no eligible image keeps the current URL. Cancellation is propagated.
- Home hero/carousel fills only the banner from the picker, and details resolves only the displayed backdrop. Custom exact-season artwork without a direct TMDB/IMDb item ID is not replaced using an unrelated franchise ID. TMDB-sourced artwork can use an IMDb ID supplied by details.
- Phone hero previously passed the catalog URL directly to Coil (including `w1280` backdrops/`w500` posters) and used viewport-sized decoding. Its artwork URL now requests TMDB `original`, with an explicit `Size.ORIGINAL` request for the hero and its prefetch. This fixes both source size and decode size at the hero call site, without touching shared image decoding.

## Proof

`backdrop-max-proof/dimensions.json` contains full before/after URLs and **downloaded image** dimensions, using the same language/area/vote policy and the existing key read-only. Raw `/images` responses and a repeatable read-only measurement script are included (no key saved). These are source-image measurements, not claims of a device screenshot or of perceptual sharpness.

| Title | Before | Selected after |
| --- | --- | --- |
| Samurai Champloo | `/original/aZRGh7ccNAdqQWhHypvlT9UuXjZ.jpg`, 1920×1080 | `/original/s6n7WhTmKcucYkyCSCLjUvTei2Y.jpg`, 3840×2160 |
| Severance | `/original/ixgFmf1X59PUZam2qbAfskx2gQr.jpg`, 3840×2160 | `/original/npD65vPa4vvn1ZHpp3o05A5vdKT.jpg`, 3840×2160 |
| Parasite | `/original/TU9NIjwzjoKPwQHoHshkFcQUCG.jpg`, 2732×1536 | `/original/cI1RBfqXbWaITTjcKGYLhd9F083.jpg`, 3840×2160 |

All image URLs use `https://image.tmdb.org/t/p/`. The requested largest-first rule selects the Champloo 3840×2160 image, not the smaller 3072×1728 redraw; dimensions alone cannot guarantee the sharpest artwork. Live TMDB results may change over time.

## Verification / integration

- PC required-lock Gradle `:composeApp:desktopTest --tests '*TmdbBackdropPickerTest' --tests '*HomeHeroMetaRepositoryTest'`: **12 tests pass**, full desktop sources compile; native frame-copy test also passes. Phone helper compiled directly from the phone worktree with platform/settings/URL stubs and cached Kotlin/JVM dependencies: **7 identical picker tests pass**. This is not a full phone UI compile.
- Focused tests cover largest/most-voted textless selection, IMDb lookup, caching, no-key/offline/parse/no-results fallback, cancellation, exact-season protection, and original-size phone URL normalization; hero merge test checks that only the banner changes.
- No live app/config/launcher changes, no push or IPA publication. No real-device or app screenshot taken; iOS UI/device verification is unavailable on this Linux host.
- Runtime resources changed: **none**. Documentation JSON files are not packaged resources. Shared `core/ui/AsyncImage*` files are untouched.

### Changed runtime class roots (include nested classes)

Both branches, under `com/nuvio/app/`:

- `features/tmdb/TmdbBackdrops*`, `features/tmdb/TmdbBackdropPicker*`, `features/tmdb/TmdbBackdropPickerKt*` (new).
- `features/home/HomeHeroMeta*` (data class), `features/home/HomeHeroMetaRepository*`, `features/home/HomeHeroMetaRepositoryKt*`.
- `features/details/MetaDetailsScreenKt*`, `features/details/ComposableSingletons$MetaDetailsScreenKt*` (regenerated call-site lambdas).

Phone additionally: `features/home/components/HomeHeroSectionKt*`, `features/home/components/ComposableSingletons$HomeHeroSectionKt*`.

The PC live-overlay whitelist must explicitly include the new helper roots, `HomeHeroMeta*` (not just the repository), and details screen roots. This branch does **not** change/reapply the live overlay script.

### Public/internal signature changes and callers

- `HomeHeroMeta` adds optional `backdrop`: constructor/default constructor, `copy`/`copy$default`, generated components change. Production callers: `HomeHeroMetaRepository` and `HomeHeroMetaRepositoryKt` in the same source file; focused caller: `HomeHeroMetaRepositoryTest`. `HomeHeroState`, repository `load`, and `withHeroMeta` signatures are unchanged.
- New internal `TmdbBackdrops.resolve` and `TmdbBackdropPicker` constructor/`resolve`: callers `HomeHeroMetaRepository`, `MetaDetailsScreenKt`, and `TmdbBackdropPickerTest`.
- New internal `largestTextlessBackdrop`: callers `TmdbBackdropPicker`, `TmdbBackdropPickerTest`. New internal `originalHeroArtworkUrl`: callers phone `HomeHeroSectionKt` and `TmdbBackdropPickerTest` (PC helper test only).
- No other existing public/internal signatures change. `MetaDetailsScreen` parameters and phone hero public APIs stay unchanged.
