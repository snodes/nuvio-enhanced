# iOS imported subtitle font fix

Changed from the swiped-brightness fix on text-subs-first.

- Imported fonts that iOS already ships (Times New Roman Bold from `timesbd.ttf`)
  now reach libass. iOS refuses to register such a file ("GSFont:
  TimesNewRomanPS-BoldMT already exists"), so `registerCustomFont` returned nil
  and `sub-font` stayed on the CJK baseline font: plain sans subtitles. The face
  name is now read from the file whether or not registration succeeds; libass
  loads the file itself from `sub-fonts-dir`.
- CI job "Simulator subtitle render" (`iosApp/tests/SubtitleRender/run.sh`) runs
  the shipped resolver plus libmpv on an iOS simulator with the phone's saved
  style and uploads `ios-subtitle-render.png` and libass's font-selection log.
