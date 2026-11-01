# Fix poster loading and 4K art flashes

Changed from the consolidated `jacob` phone branch (the details-backdrop-quality fix).

- **Long-press preview:** the shelf snapshot now stays under the sharp poster. Before, it was removed the moment the sharp poster loaded, while Coil was still fading it in, so the grey card showed for a few frames.
- **Details backdrop:** the page used to draw the addon's 1080p backdrop, wait 250 ms, then ask TMDB, so the 1080p art flashed before the 4K pick. It now reuses the home hero's pick instantly (`TmdbBackdropPicker.cached`). For titles not looked up yet, it shows no artwork for up to 1 s while TMDB answers, then the original if TMDB is slow.
- **Picker lock:** the cache lock no longer covers the network fetch, so a lookup in flight can't hide earlier picks (Sol review finding).

Validation: 8 picker tests pass (compiled directly with Kotlin; no Android SDK on the PC). Sol high review; its one finding is fixed. Screens compile only in the iOS build.

Art research (no code change): Attack on Titan's pick is the sharpest of TMDB's 26 4K backdrops by a measured-detail check, and the phone hero shows it at about 1.1 source pixels per screen pixel, so its softness is the artwork itself. Fruits Basket tops out at 1920×1080 on TMDB, Fanart.tv and TVDB; Kitsu's covers are 3360×800 strips.

## Tall key art and English logos

- **Phone banner art:** one TMDB request (`append_to_response=images,external_ids`) now returns the textless backdrop, the textless tall key art and English logos. On tall phone heroes the banner uses the key art when it is at least 1500 px wide, which gives more source pixels per screen pixel than a 4K backdrop cropped to a third of its width (about 1.45 vs 1.10; Fruits Basket's 1080p backdrop was 0.55). Wide and tablet heroes keep the backdrop; the details page keeps the backdrop.
- **English logos:** Fanart.tv's most-liked English logo, else TMDB's best English PNG, else the addon's. AIOMetadata's public server caches one logo per show for all users regardless of language, so its "English Art Only" setting (turned on 2026-11-01) can't fix it alone; AoT came back French.
- **Fanart.tv key:** new `FANART_API_KEY` in `local.properties`, generated into `TmdbConfig`. Blank skips Fanart.tv (GitHub CI builds don't have it yet, so they use TMDB logos).
- Rejected: Wallhaven as a 4K source. Its title search returned other shows for Fruits Basket and mostly fan art for AoT.
