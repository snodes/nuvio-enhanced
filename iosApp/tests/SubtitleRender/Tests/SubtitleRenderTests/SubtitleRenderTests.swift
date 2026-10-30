import CoreGraphics
import CoreText
import ImageIO
import Libmpv
@testable import SubtitleFontResolver
import UniformTypeIdentifiers
import XCTest

/// Renders the phone's saved style (Times New Roman Bold import, Bold on, 10sp, outline 2,
/// shadow strength 1, offset 20) through libmpv's software renderer and writes the PNG plus
/// mpv's font-selection log to $SUBTITLE_RENDER_OUT (TEST_RUNNER_SUBTITLE_RENDER_OUT in xcodebuild).
/// The app's FFmpeg has no PNG/JPEG encoder, so the frame is encoded with ImageIO here.
final class SubtitleRenderTests: XCTestCase {
    private let width = 1920
    private let height = 1080

    func testImportedTimesNewRomanBoldReachesLibass() throws {
        let fixtures = try XCTUnwrap(Bundle.module.url(forResource: "Fixtures", withExtension: nil))
        let output = URL(fileURLWithPath: ProcessInfo.processInfo.environment["SUBTITLE_RENDER_OUT"]
            ?? NSTemporaryDirectory())
        try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)

        // Same layout as SubtitleFontFiles.ios.kt: imported files live in one folder that
        // is also mpv's sub-fonts-dir.
        let fontsDir = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathComponent("subtitle_fonts", isDirectory: true)
        try FileManager.default.createDirectory(at: fontsDir, withIntermediateDirectories: true)
        let imported = fontsDir.appendingPathComponent("timesbd.ttf")
        try FileManager.default.copyItem(at: fixtures.appendingPathComponent("timesbd.ttf"), to: imported)

        var report: [String] = []
        let system = CTFontCreateWithName("TimesNewRomanPS-BoldMT" as CFString, 12, nil)
        let systemURL = CTFontDescriptorCopyAttribute(CTFontCopyFontDescriptor(system), kCTFontURLAttribute) as? URL
        report.append("system lookup: \(CTFontCopyPostScriptName(system)) at \(systemURL?.path ?? "nil")")

        // MPVSubtitleFontController.desiredFamily(): the imported face, else the baseline.
        let customName = MPVSubtitleFontResolver.registerCustomFont(atPath: imported.path)
        let family = customName ?? MPVSubtitleFontResolver.baselineFamily ?? "sans-serif"
        report.append("registerCustomFont: \(customName ?? "nil") -> sub-font=\(family)")

        var pixels = [UInt8](repeating: 0, count: width * height * 4)
        report += render(
            video: fixtures.appendingPathComponent("background.mp4"),
            subtitle: fixtures.appendingPathComponent("cue.srt"),
            fontsDir: fontsDir,
            family: family,
            into: &pixels
        )
        try report.joined(separator: "\n").write(
            to: output.appendingPathComponent("ios-subtitle-render.log"), atomically: true, encoding: .utf8
        )
        print(report.joined(separator: "\n"))

        XCTAssertTrue(pixels.contains { $0 != 0 }, "mpv rendered an empty frame")
        try writePNG(pixels, to: output.appendingPathComponent("ios-subtitle-render.png"))

        XCTAssertEqual(customName, "Times New Roman Bold")
        XCTAssertTrue(
            report.contains { $0.contains("fontselect") && $0.contains("Times New Roman") && $0.contains("Bold") },
            "libass did not select Times New Roman Bold"
        )
    }

    /// Mirrors MPVPlayerBridge.applySubtitleStyle with PlayerEngine.ios.kt's conversions:
    /// size 10sp * 3, sub-pos 100 - 20 / 2, shadow 1 * 1.5 with the bridge's shadow colour.
    private func render(video: URL, subtitle: URL, fontsDir: URL, family: String, into pixels: inout [UInt8]) -> [String] {
        let ctx = mpv_create()!
        defer { mpv_terminate_destroy(ctx) }
        let options: [(String, String)] = [
            ("vo", "libmpv"), ("ao", "null"), ("hwdec", "no"), ("pause", "yes"), ("keep-open", "yes"),
            ("sub-files", subtitle.path),
            ("sub-fonts-dir", fontsDir.path), ("sub-font", family),
            ("sub-ass-override", "force"),
            ("sub-color", "#FFFFFFFF"),
            ("sub-back-color", "#B3000000"),
            ("sub-outline-color", "#FF000000"),
            ("sub-border-style", "outline-and-shadow"),
            ("sub-shadow-offset", "1.5"),
            ("sub-bold", "yes"),
            ("sub-outline-size", "2"),
            ("sub-font-size", "30"),
            ("sub-pos", "90"),
        ]
        for (name, value) in options {
            XCTAssertEqual(mpv_set_option_string(ctx, name, value), 0, "option \(name)")
        }
        mpv_request_log_messages(ctx, "v")
        XCTAssertEqual(mpv_initialize(ctx), 0)

        let apiType = strdup("sw")!
        defer { free(apiType) }
        var createParams = [
            mpv_render_param(type: MPV_RENDER_PARAM_API_TYPE, data: UnsafeMutableRawPointer(apiType)),
            mpv_render_param(type: MPV_RENDER_PARAM_INVALID, data: nil),
        ]
        var renderContext: OpaquePointer?
        XCTAssertEqual(mpv_render_context_create(&renderContext, ctx, &createParams), 0)
        defer { mpv_render_context_free(renderContext) }

        var args: [UnsafeMutablePointer<CChar>?] = [strdup("loadfile"), strdup(video.path), nil]
        defer { args.forEach { free($0) } }
        args.withUnsafeMutableBufferPointer { buffer in
            buffer.baseAddress!.withMemoryRebound(to: UnsafePointer<CChar>?.self, capacity: buffer.count) {
                _ = mpv_command(ctx, $0)
            }
        }

        var log: [String] = []
        var restarted = false
        let deadline = Date().addingTimeInterval(60)
        while Date() < deadline {
            let event = mpv_wait_event(ctx, 0.2)!.pointee
            if event.event_id == MPV_EVENT_LOG_MESSAGE {
                let message = event.data.assumingMemoryBound(to: mpv_event_log_message.self).pointee
                let line = "[\(String(cString: message.prefix))] \(String(cString: message.text))"
                    .trimmingCharacters(in: .newlines)
                if line.contains("libass") || line.contains("font") || line.contains("[vo")
                    || message.log_level.rawValue <= MPV_LOG_LEVEL_INFO.rawValue {
                    log.append(line)
                }
            }
            if event.event_id == MPV_EVENT_PLAYBACK_RESTART { restarted = true }
            if event.event_id == MPV_EVENT_END_FILE || event.event_id == MPV_EVENT_SHUTDOWN { break }
            if restarted && event.event_id == MPV_EVENT_NONE { break }
        }
        log.append("playback restarted: \(restarted)")

        // Render a few times so the paused first frame and its subtitle are both queued.
        let size = UnsafeMutablePointer<Int32>.allocate(capacity: 2)
        size[0] = Int32(width)
        size[1] = Int32(height)
        let stride = UnsafeMutablePointer<Int>.allocate(capacity: 1)
        stride.pointee = width * 4
        let format = strdup("rgb0")!
        defer { size.deallocate(); stride.deallocate(); free(format) }
        for _ in 0..<5 {
            _ = mpv_render_context_update(renderContext)
            pixels.withUnsafeMutableBytes { pixelPointer in
                var params = [
                    mpv_render_param(type: MPV_RENDER_PARAM_SW_SIZE, data: UnsafeMutableRawPointer(size)),
                    mpv_render_param(type: MPV_RENDER_PARAM_SW_FORMAT, data: UnsafeMutableRawPointer(format)),
                    mpv_render_param(type: MPV_RENDER_PARAM_SW_STRIDE, data: UnsafeMutableRawPointer(stride)),
                    mpv_render_param(type: MPV_RENDER_PARAM_SW_POINTER, data: pixelPointer.baseAddress),
                    mpv_render_param(type: MPV_RENDER_PARAM_INVALID, data: nil),
                ]
                let status = mpv_render_context_render(renderContext, &params)
                if status < 0 { log.append("render error \(status)") }
            }
            // Drain log messages produced by the render (libass font selection happens here).
            while true {
                let event = mpv_wait_event(ctx, 0.2)!.pointee
                if event.event_id == MPV_EVENT_NONE { break }
                if event.event_id == MPV_EVENT_LOG_MESSAGE {
                    let message = event.data.assumingMemoryBound(to: mpv_event_log_message.self).pointee
                    let line = "[\(String(cString: message.prefix))] \(String(cString: message.text))"
                        .trimmingCharacters(in: .newlines)
                    if line.contains("libass") || line.contains("font") { log.append(line) }
                }
            }
        }
        return log
    }

    private func writePNG(_ pixels: [UInt8], to url: URL) throws {
        let provider = try XCTUnwrap(CGDataProvider(data: Data(pixels) as CFData))
        let image = try XCTUnwrap(CGImage(
            width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32, bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipLast.rawValue),
            provider: provider, decode: nil, shouldInterpolate: false, intent: .defaultIntent
        ))
        let destination = try XCTUnwrap(CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil))
        CGImageDestinationAddImage(destination, image, nil)
        XCTAssertTrue(CGImageDestinationFinalize(destination))
    }
}
