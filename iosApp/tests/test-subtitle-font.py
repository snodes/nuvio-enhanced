#!/usr/bin/env python3
"""Run the real resolver with CoreText, without the iOS UI/Compose/mpv imports."""
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
resolver = (root / 'iosApp/iosApp/Player/MPVSubtitleFontResolver.swift').read_text()
resolver = resolver.split('// MARK: - Player wiring')[0]
resolver = '\n'.join(line for line in resolver.splitlines()
                     if line not in ('import ComposeApp', 'import Libmpv', 'import UIKit'))
checks = r'''
func checkImportedFace(_ path: String) {
    let descriptors = CTFontManagerCreateFontDescriptorsFromURL(URL(fileURLWithPath: path) as CFURL) as! [CTFontDescriptor]
    let font = CTFontCreateWithFontDescriptor(descriptors[0], 12, nil)
    let fullName = CTFontCopyFullName(font) as String
    let family = CTFontCopyFamilyName(font) as String
    precondition(MPVSubtitleFontResolver.registerCustomFont(atPath: path) == fullName)
    // The already-registered path must also work on later style applications.
    precondition(MPVSubtitleFontResolver.registerCustomFont(atPath: path) == fullName)
    print("PASS imported face: \(fullName) (family: \(family))")
}
checkImportedFace(CommandLine.arguments[1])

// macOS supplies a real Times New Roman Bold face; copy it into an import folder
// rather than checking a proprietary font binary into the repository.
let times = CTFontCreateWithName("TimesNewRomanPS-BoldMT" as CFString, 12, nil)
precondition(CTFontCopyPostScriptName(times) as String == "TimesNewRomanPS-BoldMT")
precondition(CTFontGetSymbolicTraits(times).contains(.traitBold))
let timesURL = CTFontDescriptorCopyAttribute(CTFontCopyFontDescriptor(times), kCTFontURLAttribute) as! URL
let importDir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
try FileManager.default.createDirectory(at: importDir, withIntermediateDirectories: true)
defer { try? FileManager.default.removeItem(at: importDir) }
let importedTimes = importDir.appendingPathComponent("timesbd.ttf")
try FileManager.default.copyItem(at: timesURL, to: importedTimes)
checkImportedFace(importedTimes.path)
let selected = MPVSubtitleFontResolver.registerCustomFont(atPath: importedTimes.path)!
precondition(selected != CTFontCopyFamilyName(times) as String, "must not collapse Bold to its family")
let selectedFont = CTFontCreateWithName(selected as CFString, 12, nil)
precondition(CTFontGetSymbolicTraits(selectedFont).contains(.traitBold))
precondition(MPVSubtitleFontResolver.registerCustomFont(atPath: importDir.appendingPathComponent("missing.ttf").path) == nil)
precondition(MPVSubtitleFontResolver.customFontsDirectory?.lastPathComponent == "subtitle_fonts")
print("PASS repeated import, exact bold face, invalid file and shared import directory")
'''
with tempfile.TemporaryDirectory(prefix='nuvio-subtitle-font-') as tmp:
    script = Path(tmp) / 'main.swift'
    script.write_text(resolver + checks)
    subprocess.run(['swift', str(script), str(root / 'iosApp/iosApp/SubtitleFonts/NotoSans-Regular.ttf')], check=True)
