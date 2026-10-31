// swift-tools-version: 6.2

import PackageDescription

// Tests that use the mocks generated for the sample's Kotlin framework. Build the
// framework first: ./gradlew -p sample assembleSharedDebugXCFramework
let package = Package(
    name: "SampleTests",
    platforms: [
        .macOS(.v14),
        .iOS(.v16),
    ],
    dependencies: [
        // Until a swift-mockable release treats module-qualified parents as conformances.
        .package(url: "https://github.com/yysskk/swift-mockable.git", branch: "main"),
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "../shared/build/XCFrameworks/debug/Shared.xcframework"
        ),
        .testTarget(
            name: "SharedTests",
            dependencies: [
                "Shared",
                .product(name: "Mockable", package: "swift-mockable"),
            ],
            swiftSettings: [
                .swiftLanguageMode(.v6),
            ]
        ),
    ]
)
