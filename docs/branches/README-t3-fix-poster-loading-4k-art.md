# Fix poster loading and 4K art flashes

Changed from the consolidated `jacob` phone branch (the details-backdrop-quality fix).

- **Long-press preview:** the shelf snapshot now stays under the sharp poster. Before, it was removed the moment the sharp poster loaded, while Coil was still fading it in, so the grey card showed for a few frames.
- **Details backdrop:** the page used to draw the addon's 1080p backdrop, wait 250 ms, then ask TMDB, so the 1080p art flashed before the 4K pick. It now reuses the home hero's pick instantly (`TmdbBackdropPicker.cached`). For titles not looked up yet, it shows no artwork for up to 1 s while TMDB answers, then the original if TMDB is slow.
- **Picker lock:** the cache lock no longer covers the network fetch, so a lookup in flight can't hide earlier picks (Sol review finding).

Validation: 8 picker tests pass (compiled directly with Kotlin; no Android SDK on the PC). Sol high review; its one finding is fixed. Screens compile only in the iOS build.

Art research (no code change): Attack on Titan's pick is the sharpest of TMDB's 26 4K backdrops by a measured-detail check, and the phone hero shows it at about 1.1 source pixels per screen pixel, so its softness is the artwork itself. Fruits Basket tops out at 1920×1080 on TMDB, Fanart.tv and TVDB; Kitsu's covers are 3360×800 strips.
