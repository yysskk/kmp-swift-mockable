# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-31

The first release. It requires swift-mockable 4.0 or later, which treats a module-qualified parent protocol as a conformance.

### Added

- `swift-generator`, a Swift package that reads the protocols of a Kotlin/Native framework from its synthesized Swift interface. A `suspend` function's completion-handler requirement is dropped when Swift also imports it as `async`, since both name the same Objective-C method.
- `MirrorGenerator`, which writes a `@Mockable` protocol mirroring each requested Kotlin interface. A mirror refines the interface it restates (`protocol UserRepository: Shared.UserRepository`), lists the first mirrored parent so that its mock is subclassed, and restates the requirements of every other parent. The `condition:` of `@Mockable` and the access level of the mirrors are configurable.
- The `kmp-swift-mockable-generator` command-line tool. It runs `swift-synthesize-interface` on a framework (`--framework`, `--target`, `--sdk`) or reads an interface file (`--interface-file`, `--module`), mirrors the interfaces listed in `--interfaces`, warns about names the framework does not export, and writes `--output` only when its contents change.
- The `@SwiftMockable` annotation, published as `io.github.yysskk:kmp-swift-mockable-annotations` for every Kotlin target.
- A symbol processor, published as `io.github.yysskk:kmp-swift-mockable-ksp`, that lists the interfaces annotated with `@SwiftMockable` in `kmp-swift-mockable/interfaces.json` with the name Kotlin/Native exports each one under, honoring `@ObjCName` and nesting. Applying the annotation to anything but an interface is a compile error.
- The `io.github.yysskk.kmp-swift-mockable` Gradle plugin. Applied next to the Kotlin Multiplatform and KSP plugins, it adds the annotation and the symbol processor, and generates `<FrameworkName>Mocks.swift` after each Apple framework links. `kmpSwiftMockable { outputDir; condition; accessLevel }` configures the file, and `generateSwiftMocks` generates it from the host simulator's debug framework.
- The plugin's jar bundles the generator as a universal macOS binary, extracted on first use to the Gradle user home. `kmpSwiftMockable.generatorExecutable` runs another build of it instead.
- A release workflow that publishes the annotation and the processor to Maven Central, and the plugin to the Gradle Plugin Portal, when a version tag is pushed.

[Unreleased]: https://github.com/yysskk/kmp-swift-mockable/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yysskk/kmp-swift-mockable/releases/tag/v1.0.0
