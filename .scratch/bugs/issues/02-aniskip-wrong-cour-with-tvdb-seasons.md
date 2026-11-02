# AniSkip can pick the wrong cour under TVDB seasons

Status: open

- **What broke:** anime episodes now use one IMDb id with TVDB seasons (AIOMetadata change, 2026-11-01). AniSkip lookups work out the MAL entry by season. TVDB puts several MAL entries in one season (Bleach Thousand-Year Blood War is all of season 17, three MAL entries).
- **Repro:** play Bleach S17E14 and check which MAL id the skip lookup uses.
- **Expected:** the cour that contains episode 14 (the second cour), not the first.
