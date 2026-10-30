// swift-tools-version:5.9

// Simulator harness: renders one SRT cue through the app's libmpv/libass build with an
// imported subtitle font. run.sh copies the app's MPVSubtitleFontResolver into
// Sources/SubtitleFontResolver before building, so the test exercises the shipped code.
import PackageDescription

let package = Package(
    name: "SubtitleRender",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "SubtitleFontResolver", targets: ["SubtitleFontResolver"]),
    ],
    dependencies: [
        .package(path: "../../../MPVKit"),
    ],
    targets: [
        .target(name: "SubtitleFontResolver"),
        .testTarget(
            name: "SubtitleRenderTests",
            dependencies: [
                "SubtitleFontResolver",
                .product(name: "MPVKit", package: "MPVKit"),
            ],
            resources: [.copy("Fixtures")]
        ),
    ]
)
