# Phone CI fix

## Changed from ca0f27cf

- Import the shared TMDB backdrop picker on details; preserve largest textless artwork selection.
- Restore the desktop shared audio-language matcher used by commentary exclusion.
- Run audio-original/commentary and backdrop regression tests in the branch iOS workflow.

## Verification

- Whole commonMain Kotlin metadata compilation passed locally with Java 21, the shared build lock, no daemon/cache, two workers and 3 GiB heap.
- Cross-source-set import scan found no project imports requiring non-common-only symbols.
- Android compilation was attempted but this Linux host has no Android SDK; Apple targets require macOS cinterop toolchains. The branch workflow validates Android host tests, iOS Release compilation and simulator subtitle rendering.
