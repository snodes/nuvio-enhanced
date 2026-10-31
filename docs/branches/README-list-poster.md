# Poster-first list artwork

## Changed from 9e64d6c5 (text-subs-first)
- Continue Watching's Wide (list-like) card always chooses the show's poster before episode screenshots, independent of the episode-thumbnail preference.
- Both episode List render paths now pass the show's poster explicitly; screenshots/backgrounds are used only when it is absent or blank. Poster art is not blurred as an unwatched-episode spoiler.
- Add focused shared artwork selection tests. Other card styles/layout geometry are unchanged. No player files changed.

## Verification and deployment
The identical selection helper/tests are exercised by the desktop artwork branch. A separate focused JVM harness also compiles and tests this phone worktree's own helper. Full iOS UI compilation and on-device screenshots remain unverified on this Linux host. No IPA built/published; no live settings changed.
