# Handoff: hero art picks and one-page-per-show anime (2026-11-01)

Written for a fresh thread. Read `docs/branches/README-t3-fix-poster-loading-4k-art.md` first; it has the full history of this work.

## Where things are

- Code: branch `t3/fix-poster-loading-4k-art` (worktree `~/.t3-trial/worktrees/nuvio-enhanced/t3-77d6f649`). The `jacob` branch (main checkout `~/Projects/nuvio/nuvio-enhanced`) is fast-forwarded to it and pushed to the `snodes` remote. Never push to `origin` (upstream).
- Jacob's phone runs the build from 6:22 PM on Nov 1: textless TMDB tall key art on tall phone home heroes, TMDB English logos, no 1080p flash on details, long-press fix.
- Build an IPA: `nuvio-vm-build --force .` from the main checkout on `jacob` (~30 min; see `~/claude-memory/nuvio-vm-build.md`). Building from a worktree breaks the IPA copy step because of the `/` in the branch name.
- Tests: no Android SDK on the PC. `/tmp/nv/jvmtest` compiles `TmdbBackdropPicker.kt` and its test straight with the cached Kotlin 2.4.10 jars (`comp.txt`, `lib.txt`, stubs in `src/`). Copy the changed files into `src/` and rerun.

## Open problem 1: the tall-art pick is a bad heuristic

`TmdbBackdropPicker.fetch` (features/tmdb/TmdbBackdropPicker.kt) picks the largest textless TMDB poster ≥1500 px wide as `tallArt`, which then *always* replaces the banner on tall phone heroes (`heroArtworkUrl` in HomeHeroSection.kt). Jacob's phone results:

- **Fruits Basket:** better (it only has a 1080p backdrop).
- **Hunter x Hunter:** downgraded. The pick is `/lVhM1HdJW8Yv07b20PDqTIlxGJi.jpg` (1603×2406, 1 vote), a black-and-white pencil fan sketch. All of HxH's textless posters have 1 vote, so votes can't rank them.
- **My Hero Academia:** downgraded. It picked Season 1 key art (2000×3000, 3 votes), which looks worse than the 4K backdrop it replaced and doesn't match the details page.

Textless posters on TMDB are often fan art or sketches, and size isn't quality. Options to weigh:
- Use tall art only when the backdrop is under 4K (it only helped Fruits Basket). This is the smallest change.
- Require a minimum vote count or vote average.
- Drop tall art and solve Fruits Basket another way.

Check AoT, Fruits Basket, Samurai Champloo, HxH and MHA after any change.

## Open problem 2: the Fruits Basket details page is still soft

The details page deliberately keeps the backdrop (MetaDetailsScreen.kt, ~line 1160), and Fruits Basket's best backdrop is 1920×1080 on TMDB, Fanart.tv and TVDB. If problem 1 keeps tall art for under-4K backdrops, the details hero could use the same rule. That fits only if the details hero is tall on phones; check its geometry first.

## Next feature: one page per show, season dropdown, per-season art and ratings

Jacob's idea: the per-season pages are what cause mismatched art (Season 2 art on Season 1; every Bleach cour sharing one art). Today anime arrive with one Kitsu/AniList ID per season from AIOMetadata, so each season gets its own page.

- **One page:** have AIOMetadata send anime as one title per show (IMDb/TMDB IDs), like regular TV. The app already has a season dropdown (`DetailSeriesContent.kt`). Look for an AIOMetadata setting first; Jacob's config is the remote one used earlier in this thread. Risk: episode numbering, since some anime are numbered differently on TMDB than on stream sources (One Piece, Bleach's later arcs). Check that AIOStreams (Jacob's addon, `u/jacob`) still finds streams.
- **Art follows the dropdown:** picking a season swaps the hero to that season's TMDB season poster (`tv/{id}/season/{n}` images). Backdrops are show-level only.
- **Ratings follow the dropdown:** MAL/AniList scores switch to the selected season. IMDb and Rotten Tomatoes stay whole-show. Map AniList/MAL entries to TMDB/TVDB seasons and episode ranges with a public mapping list (Fribb's anime-list is the common one). A season split into cours (Bleach TYBW) has several entries.
- **Per-season ratings strip (Jacob's own MyAnimeList scores):** a strip showing Jacob's personal MAL list score (1–10) for every season at once. MAL lists each season or cour as its own entry, so his scores already exist per season. It stays visible when switching seasons, highlights the current season, and tapping one switches the season. Each cour (Bleach TYBW parts) is its own MAL entry with its own score. The app has rating providers (`features/tracking/TrackingRatings.kt`, `anilist/AniListRatingsProvider.kt`; AniList, Trakt and Simkl) but no MyAnimeList provider. This needs a MAL login (MAL API v2 OAuth, `GET /users/@me/animelist?fields=list_status`) and, if scores can be set from the strip, `PATCH /anime/{id}/my_list_status`. Ask whether AniList (which can mirror MAL) would do instead. Design mock: `docs/handoffs/mock/season-ratings.html` (screenshot `season-ratings.png` beside it), with three layouts (chips, bars, line). Jacob hasn't picked one yet. Ask him before building.

Order suggestion: fix problem 1 first (small, ships in the next IPA), then prove the one-page switch with AIOMetadata on MHA and Bleach before writing any app code.
