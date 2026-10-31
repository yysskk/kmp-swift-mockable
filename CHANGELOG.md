# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `swift-generator`, a Swift package that reads the protocols of a Kotlin/Native framework from its synthesized Swift interface. A `suspend` function's completion-handler requirement is dropped when Swift also imports it as `async`, since both name the same Objective-C method.
- `MirrorGenerator`, which writes a `@Mockable` protocol mirroring each requested Kotlin interface. A mirror refines the interface it restates (`protocol UserRepository: Shared.UserRepository`), lists the first mirrored parent so that its mock is subclassed, and restates the requirements of every other parent. The `condition:` of `@Mockable` and the access level of the mirrors are configurable.
- The `kmp-swift-mockable-generator` command-line tool. It runs `swift-synthesize-interface` on a framework (`--framework`, `--target`, `--sdk`) or reads an interface file (`--interface-file`, `--module`), mirrors the interfaces listed in `--interfaces`, warns about names the framework does not export, and writes `--output` only when its contents change.
