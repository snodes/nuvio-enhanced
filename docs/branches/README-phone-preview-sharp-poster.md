# Phone preview sharp poster

## Changed from 93893a44

- Replace the permanently enlarged shelf layer in the context-menu preview with a separately decoded poster.
- Promote TMDB thumbnails to w780, preserve larger/original images and other addon URLs, and use smooth image filtering.
- Use a smoothly filtered snapshot of the shelf only while the preview image loads, retaining the existing lift and return animation.
- Add focused shared URL tests. Linux validation results are recorded below.

## Validation

- Kotlin 2.4.10 JVM compilation of the changed Compose overlay, shared URL resolver, and tests passed using the Compose compiler plugin, cached desktop app/dependency classes, and the matching Backdrop desktop artifact. JUnit passed all 3 tests. This focused compile is not a full application build.
- `git diff --check` passed.
- Project Gradle compilation and host tests were attempted, but configuration stops because this Linux environment has no Android SDK. This mobile checkout defines Android and iOS targets only, with no desktop target.
- SDK setup blocker is tracked in Wisteria ticket 471.
- iOS linking and real-device animation checks require macOS/the phone and remain unverified.
- The adjacent desktop checkout uses the same URL and lifted-layer drawing path, with default image filtering; it has no separate preview-size promotion. The phone fix avoids depending on backend-specific scaling of that grid layer.
