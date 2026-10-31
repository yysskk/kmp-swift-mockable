// swift-tools-version: 6.2

import PackageDescription

let package = Package(
    name: "kmp-swift-mockable-generator",
    platforms: [
        .macOS(.v13),
    ],
    products: [
        .executable(
            name: "kmp-swift-mockable-generator",
            targets: ["kmp-swift-mockable-generator"]
        ),
    ],
    dependencies: [
        .package(url: "https://github.com/swiftlang/swift-syntax.git", from: "604.0.0"),
        .package(url: "https://github.com/apple/swift-argument-parser.git", from: "1.5.0"),
    ],
    targets: [
        .target(
            name: "KMPSwiftMockableGenerator",
            dependencies: [
                .product(name: "SwiftSyntax", package: "swift-syntax"),
                .product(name: "SwiftParser", package: "swift-syntax"),
            ]
        ),
        .executableTarget(
            name: "kmp-swift-mockable-generator",
            dependencies: [
                "KMPSwiftMockableGenerator",
                .product(name: "ArgumentParser", package: "swift-argument-parser"),
            ]
        ),
        .testTarget(
            name: "KMPSwiftMockableGeneratorTests",
            dependencies: ["KMPSwiftMockableGenerator"]
        ),
    ]
)
