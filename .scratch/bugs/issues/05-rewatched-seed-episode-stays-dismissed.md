# A removed Next Up card can stay hidden after rewatching its episode

Status: open

- **What:** removals now merge across phone and desktop (add-only). Watching a show again clears its removal keys on that device, but the next sync merges the other device's copy back.
- **Repro:** remove Vinland Saga's Next Up card (key `tt10233448|1|1`) on the phone. Rewatch S1E1 on the desktop. After the next sync, the S1E2 Next Up card is still hidden.
- **Only this case:** watching a later episode makes a new key, so the card comes back as it should. A fix needs synced un-removals (removal times or tombstones).
