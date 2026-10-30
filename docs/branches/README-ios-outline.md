# iOS subtitle outline parity

Changed from the imported subtitle font fix on text-subs-first.

- Scale the saved outline and shadow with mpv font size to Harbor's border 1 / shadow 1.4 at font 32; use opaque black shadow.
- Make blur and margin scaling explicit without adding settings.
- Simulator proof compares the imported Times Bold style against independently normalized Harbor options, asserting identical pixels.
