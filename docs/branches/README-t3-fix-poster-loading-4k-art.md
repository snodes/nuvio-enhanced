# Fix poster loading and 4K art flashes

Changed from the consolidated `jacob` phone branch (the details-backdrop-quality fix).

- **Long-press preview:** the shelf snapshot now stays under the sharp poster. Before, it was removed the moment the sharp poster loaded, while Coil was still fading it in, so the grey card showed for a few frames.
- **Details backdrop:** the page used to draw the addon's 1080p backdrop, wait 250 ms, then ask TMDB, so the 1080p art flashed before the 4K pick. It now reuses the home hero's pick instantly (`TmdbBackdropPicker.cached`). For titles not looked up yet, it shows no artwork for up to 1 s while TMDB answers, then the original if TMDB is slow.
- **Picker lock:** the cache lock no longer covers the network fetch, so a lookup in flight can't hide earlier picks (Sol review finding).

Validation: 8 picker tests pass (compiled directly with Kotlin; no Android SDK on the PC). Sol high review; its one finding is fixed. Screens compile only in the iOS build.

Art research (no code change): Attack on Titan's pick is the sharpest of TMDB's 26 4K backdrops by a measured-detail check, and the phone hero shows it at about 1.1 source pixels per screen pixel, so its softness is the artwork itself. Fruits Basket tops out at 1920×1080 on TMDB, Fanart.tv and TVDB; Kitsu's covers are 3360×800 strips.

## Tall key art and English logos

- **Phone banner art:** one TMDB request (`append_to_response=images`) now returns the textless backdrop, the textless tall key art and English logos. On tall phone heroes the banner uses the key art when it is at least 1500 px wide, which gives more source pixels per screen pixel than a 4K backdrop cropped to a third of its width (about 1.45 vs 1.10; Fruits Basket's 1080p backdrop was 0.55). Wide and tablet heroes keep the backdrop; the details page keeps the backdrop.
- **English logos:** TMDB's top-rated English PNG logo from that same request, else the addon's; kept off when the TMDB setting shows titles in their original language. AIOMetadata's public server caches one Fanart.tv logo per show for every user regardless of language, so its "English Art Only" setting (turned on 2026-11-01) can't fix it alone; AoT came back French. Fanart.tv logos were tried first and dropped: they needed a second request and key for no visible gain.
- Validation: 10 picker tests pass (direct Kotlin compile). Sol high review found a shared-timeout issue (moot once Fanart.tv was dropped) and the original-language logo rule, now fixed.
- Rejected: Wallhaven as a 4K source. Its title search returned other shows for Fruits Basket and mostly fan art for AoT.
