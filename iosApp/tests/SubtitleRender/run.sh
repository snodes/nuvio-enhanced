#!/usr/bin/env bash
# Render the phone's subtitle style on an iOS simulator and write
# ios-subtitle-render.png/.log to $1 (default: build/subtitle-render).
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd -P)"
root="$(cd "${here}/../../.." && pwd -P)"
out="$(mkdir -p "${1:-${root}/build/subtitle-render}" && cd "${1:-${root}/build/subtitle-render}" && pwd -P)"

mkdir -p "${here}/Sources/SubtitleFontResolver"
# Copy the shipped resolver without its app-only imports and player wiring.
python3 - "${root}/iosApp/iosApp/Player/MPVSubtitleFontResolver.swift" \
    "${here}/Sources/SubtitleFontResolver/MPVSubtitleFontResolver.swift" <<'EOF'
import sys
source = open(sys.argv[1]).read().split('// MARK: - Player wiring')[0]
lines = [l for l in source.splitlines() if l not in ('import ComposeApp', 'import Libmpv', 'import UIKit')]
open(sys.argv[2], 'w').write('\n'.join(lines) + '\n')
EOF

udid="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
devices = json.load(sys.stdin)["devices"]
ios = sorted(((runtime, d) for runtime, ds in devices.items() if "iOS" in runtime for d in ds if d["name"].startswith("iPhone")), key=lambda x: x[0])
print(ios[-1][1]["udid"])')"
echo "simulator ${udid}"

cd "${here}"
scheme="$(xcodebuild -list -json | python3 -c '
import json, sys
schemes = json.load(sys.stdin)["workspace"]["schemes"]
print("SubtitleRender-Package" if "SubtitleRender-Package" in schemes else "SubtitleRender")')"
set +e
TEST_RUNNER_SUBTITLE_RENDER_OUT="${out}" xcodebuild test \
    -scheme "${scheme}" \
    -destination "id=${udid}" \
    -derivedDataPath "${root}/build/subtitle-render-dd" \
    2>&1 | tee "${out}/xcodebuild.log" | grep -E 'error:|Test Case|\[|registerCustomFont|system lookup|\*\* TEST'
status="${PIPESTATUS[0]}"
set -e
exit "${status}"
