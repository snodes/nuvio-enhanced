# iOS ASS subtitle styling

Changed from the pause-overlay patch on text-subs-first.

- Apply the saved subtitle style to ASS with `sub-ass-override=force`. iOS has no
  original-ASS/custom-style switch; no new toggle is added.
- Preserve imported font faces, not only their families: `timesbd.ttf` selects
  Times New Roman Bold rather than the regular family face. Existing non-Latin
  script fallback is unchanged.
- Expose the existing Application Support/subtitle_fonts import directory to
  libass at startup, so its FreeType loader can read the imported font files.
- Focused macOS CoreText checks run before the iOS build; the IPA still needs
  installation/replacement and real-phone playback verification.
