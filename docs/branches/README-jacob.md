# Jacob phone branch

## Changed from ipa-sharp-art

Starts from the newest IPA build branch, retaining text-subs-first, phone-art-kitsu, sharp long-press posters, hero backdrops, original-size artwork and Kitsu history fixes. Merges ios-ass-style, then the local enhanced branch's 27 additional commits. Includes pause-overlay metadata timing, English text subtitles before PGS, forced ASS styling, night mode, skip-intro timing, PublicMetaDB, subtitle size steps, store updates and the Enhanced MPVKit fork.

## Conflict resolutions

- .github/workflows/ios.yml: retained ipa-sharp-art's newer encrypted IPA artifacts, focused audio/artwork/brightness tests and simulator subtitle render job, rather than restoring the older unencrypted artifact workflow.
- composeApp/build.gradle.kts: retained the focused Android host brightness test source directory instead of removing all host tests.
- MPVSubtitleFontResolver.swift: retained best-effort font registration and reading the full face name from the imported file even when iOS already ships that face.
- Track selection and MPV bridge merged automatically. Verified the newer text-before-PGS logic remains single-copy and sub-ass-override remains force.
- enhanced merged cleanly, retaining the above changes and adding its engine/settings changes.

## Uncommitted Xcode guard

After all commits and checks, ContentView.swift is copied byte-for-byte from ipa-sharp-art's working tree and intentionally left uncommitted. The source worktree is read only and its active build is untouched.

## Exclusions and upstream discrepancy

- archive/ios-subfont-harness: archived on purpose; its five remaining commits are excluded.
- hero-largest-backdrop and phone-preview-sharp-poster: explicitly superseded by ipa-sharp-art; their old histories are not merged.
- t3/aiometadata-curated-artwork, t3/macos-vm-part4-handoff, t3/phone-preview-sharp-poster and t3/run-first-vm-ipa-build: all point at a newer upstream checkout with 97 commits beyond the selected local enhanced. No separate requested feature commits; excluded with that newer upstream history.
- origin/enhanced is 97 commits ahead of local enhanced, so it is not the 27-commit update described in the task. This consolidation uses local enhanced, whose 27 additional commits exactly match the specified count. The newer remote upstream history, including its later 0.6.6 store bump, is not included. No fetch or upstream-branch update was performed.

## Validation

- No conflict markers; git diff --check passes.
- Focused Kotlin K2 compile of PlayerTrackSelection.kt and its tests against cached desktop dependencies passes; all 22 JUnit tests pass. Uses the same direct-compiler approach as /tmp/nuvio-phone-preview-validation/validate.py.
- No Kotlin source needed manual conflict resolution. Gradle build configuration was resolved by retaining the newer focused test setup.
- Full phone Gradle configuration is unavailable because the Android SDK is missing, already tracked by Wisteria ticket 471. iOS Swift/CoreText and IPA builds require macOS and were not run.

## Fully contained old branches

cw-relay-id, enhanced, ios-ass-style, ipa-sharp-art, pause-overlay, phone-art-kitsu, text-subs-first, xcode26-sdk-guard. Each has zero commits in jacob..<branch>. No branch was deleted.
