# Details backdrop quality

Changed from the consolidated `jacob` phone branch.

The details screen selected TMDB's largest backdrop but passed the original metadata to the visible phone and tablet heroes. Only background effects used the selected URL. Both visible heroes now receive a display-only metadata copy containing the selected backdrop. Posters, IDs, episode data, and repository metadata keep their existing values.

Validation: the full desktop build and 12 focused picker/hero repository tests pass. A Sol high read-only review found no issues. The temporary source-routing check failed before the fix and passes after it. No rendered device verification yet; source checks prove wiring, not pixels. The temporary source-routing check reproduced both disconnected hero calls before the fix and passes after it. It checks wiring, not rendered pixels.
