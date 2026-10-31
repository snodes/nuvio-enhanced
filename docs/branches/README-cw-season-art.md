# cw-season-art

## Changed from b08f5b97 (phone-port)

- Continue Watching projects fresh exact-ID art from the installed, enabled AniList/Kitsu relay catalog over stored progress/cache art on both resume and next-up items. Uses the relay's existing exact-entry logic; no title searches, hard-coded host, live configuration edits, or new relay deployment.
- Only art changes: playback identity, episode details/thumbnails and progress remain intact. Missing matching entry, disabled/uninstalled relay, empty cover or network failure retains prior behavior. Missing season banner/logo clears stale franchise art. Resets per profile/relay list; requests are bounded and cancellation propagates.
- HomeHeroMeta and TMDB backdrop picker are untouched (compatible with backdrop-max); core/ui/AsyncImage files untouched.

## Relay prerequisite / proof

The PC relay also fixes a concrete exact-season ambiguity: AniList `Page.media(idMal_in:[61469])` returns **two** Steel Ball Run parts with MAL 61469, so last-entry-wins selected the later stage. Batched aliases of `Media(idMal:)` select the canonical first-stage entry instead. This is the same logic used by the catalog and CW, not a second client implementation. The relay source change must be deployed alongside the app change; the running relay was not restarted.

`cw-season-art-proof.json` records read-only live progress poster/background URLs before, and the changed relay's real AniList response after. Steel Ball Run now selects cover `bx190327-riJNCFL7w9y4.jpg` (first stage), not the later-stage `bx210482-P1VNKbqdJ6Zj.jpg` returned by the currently running relay. One Piece selects `bx21-ELSYx3yMPcKM.jpg`. The focused Kotlin test parses captured relay-shaped payloads and verifies both artwork replacements. This is source/URL proof, not an app/device screenshot.

## Runtime roots / signatures

- Both apps: `com/nuvio/app/features/home/HomeScreenKt*` (including regenerated nested lambdas) and new `HomeContinueWatchingSeasonArtKt*`. No `HomeHeroMeta*`, model/data-class, existing function signature, packaged resource, or `AsyncImage*` changes.
- New internal functions: `rememberContinueWatchingSeasonArt(items, addons, profileId): List<ContinueWatchingItem>` (caller `HomeScreen`); `loadContinueWatchingSeasonArt(relayUrls, fetch): List<MetaPreview>` (suspend; caller the remember helper and focused test); `applyContinueWatchingSeasonArt(items, artwork): List<ContinueWatchingItem>` (caller the remember helper and focused test).
- PC relay: Python `tools/anilist-kitsu-addon/server.py` module, existing `season_art_for(mal_ids)` body and manifest version only; no Python signature changes. Caller remains `catalog(extra)`. `test_art.py` adds query/identity regression coverage.

## Verification

- PC required-lock/no-build-cache Gradle `:composeApp:desktopTest --tests '*HomeContinueWatchingSeasonArtTest'`: **7 tests pass**, full PC app and test sources compile.
- PC relay `python -m unittest -v`: **8 tests pass**; real canonical AniList query confirms first-stage SBR and current One Piece URLs recorded in proof JSON.
- No live app, relay process, launcher, live config, main checkout, or other worktree changed; no push/publication. No real app/device screenshot or full iOS compile claimed.
- Phone worktree's actual new helper and focused tests compiled directly with cached Kotlin/Compose compiler and PC model/dependency classes: **7 tests pass**. Helper/test source is byte-identical between PC and phone. This verifies shared logic, not the full iOS/phone UI build.
- Generated build directories and temporary phone compiler output removed after verification.
