# ipa-sharp-art

Changed from text-subs-first (the branch the phone runs):
- Long-press preview: loads TMDB's original poster (any size name, including AIOMetadata's
  w600_and_h900_bestv2) with smooth scaling, instead of enlarging the grid's small snapshot. The snapshot
  shows until the big poster loads; decoding is capped at 1080x1620.
- Hero/details backdrops: the largest-textless-backdrop picker now falls back to the build's own TMDB key.
  It used only a key typed into Settings, so phones without one always kept the addon's (AIOMetadata's) backdrop.
