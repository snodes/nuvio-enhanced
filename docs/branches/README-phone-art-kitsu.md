# phone-art-kitsu

Changed from `text-subs-first` (the Continue Watching poster fix).

- **Sharper artwork on iOS.** Coil 3.6.3's Skia decoder shrinks images with nearest-neighbour sampling, so
  posters and heroes looked jagged next to the PC. `PosterBitmapDecoder` (same code as the PC's
  poster-render branch) resizes with Lanczos3 for every JPEG/PNG/WebP the app loads.
- **Long-press lift on wide Continue Watching cards.** They used a plain long-press, so the action sheet
  popped up without the lift-and-zoom other posters get. They now use `posterCardClickable`, like the PC.
- **"E4" instead of "S1 E4" for Kitsu shows.** Kitsu files each season/sequel as its own entry under season 1.
  `episodeCodeLabel` drops the made-up season on Continue Watching, streams and player labels.
- **Anime history stays on Kitsu ids.** Rows backed by TMDB/IMDb gave the same show an IMDb id, which split its
  history into two Continue Watching cards (Hunter x Hunter, Haikyu!!). Saving now converts IMDb anime episodes
  to their Kitsu entry (`AnimeKitsuIds`, table generated from Fribb/anime-lists), and every watched-history pull
  folds old IMDb anime marks into Kitsu through Nuvio Sync only (trackers and media servers untouched).
  Seasons with no single Kitsu match stay on IMDb.

Not ported yet: the PC's full-size re-request for the long-press preview (the preview still enlarges the shelf image).

Tests: `AnimeKitsuIdsTest` runs in the iOS workflow's shared-test job.
