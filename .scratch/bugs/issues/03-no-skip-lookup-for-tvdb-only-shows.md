# No intro-skip lookup for TVDB-only shows

Status: open

- **What broke:** shows AIOMetadata can't match to IMDb get `tvdb:` ids. The skip lookup needs IMDb or MAL ids, so these shows get no intro or outro skip.
- **Repro:** open any anime whose page id starts with `tvdb:` and play an episode; no skip button appears.
