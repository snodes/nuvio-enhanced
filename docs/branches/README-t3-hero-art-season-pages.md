# Hero art picks and one-page anime seasons

Changed from `t3/fix-poster-loading-4k-art`. Handoff: `docs/handoffs/2026-11-01-art-and-season-pages.md`.

## Tall art only without a 4K backdrop

- **Problem:** tall phone heroes always used TMDB's largest textless poster. Those are often fan art, and their votes are too few to rank (HxH's are all 1 vote), so HxH got a pencil sketch and MHA got Season 1 key art instead of their 4K backdrops.
- **Fix:** the picker offers tall art only when the best textless backdrop is under 3840 px wide. Of AoT, Fruits Basket, Samurai Champloo, HxH and MHA (checked against TMDB on 2026-11-01), only Fruits Basket (1920×1080 best backdrop) gets tall art; the rest go back to their 4K backdrops.
- **Details page:** the phone details hero is portrait (1.33× its width), so it now uses the same tall art. Tablets and wide layouts keep the backdrop. This sharpens Fruits Basket's details page and makes it match the home hero.
- Validation: 11 picker tests pass (direct Kotlin compile, `/tmp/nv/jvmtest`).
