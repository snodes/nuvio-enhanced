# Continue Watching removals shared by phone and desktop

Changed from `jacob`.

- **Problem:** "Remove" on a Next Up card is saved in the per-device settings blob on Nuvio Sync. Phone and desktop have separate blobs ("mobile" and "desktop"), so a show removed on the phone kept showing on the desktop, and the other way round.
- **Fix:** after pulling its own settings, each app also reads the other device's blob and adds its removal keys to its own list (never drops any). The next push stores the merged list, so both lists converge. Desktop twin: nuvio-desktop `t3/shared-cw-removals`; this app always syncs as "mobile", so it reads the "desktop" blob.
- Validation: `ContinueWatchingDismissalsTest` (2 tests).
