# Published IPA version links serve the newest build

Status: open

## What broke

The fixed Nuvio.ipa link and all six currently retained versioned IPA filenames
under ~/.local/share/nuvio-source/ are hard links to the same inode. Publishing
new bytes has overwritten the content reachable through old version URLs, so
those filenames no longer preserve previous builds.

## Reproduce / evidence

Run `ls -li ~/.local/share/nuvio-source/*.ipa`. At the November 1, 2026 check,
all seven filenames had the same inode, link count seven, size 73,348,544 bytes
and modification timestamp. The current manifest describes the latest tall-art
Release build, but the older run/VM filenames expose those same bytes.

## Expected

Each retained version URL serves its own immutable IPA; only the fixed latest
alias changes when a new build is published.

## Scope

Discovered during Opus's handoff review. Publication tooling was not changed.
Inspect ~/.local/bin/nuvio-phone before implementing a separate fix; preserve
current install availability and avoid publishing another build just to reproduce.
