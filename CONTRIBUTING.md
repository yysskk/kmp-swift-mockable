# Contributing

Thanks for your interest in improving kmp-swift-mockable! This guide covers the
project layout, how to build and test, and the conventions we follow.

## Project layout

- `swift-generator/` — a Swift package with the generator that reads a
  framework's synthesized Swift interface and writes `@Mockable` mirror
  protocols.
  - `Sources/KMPSwiftMockableGenerator/` — the library: `ModuleInterface` reads
    protocols from a synthesized interface.
  - `Sources/kmp-swift-mockable-generator/` — the command-line tool.
  - `Tests/KMPSwiftMockableGeneratorTests/` — tests, with interface fixtures
    shaped like real Kotlin/Native output in `Fixtures.swift`.

## Building and testing

```sh
cd swift-generator
swift build
swift test
```

Tests use [swift-testing](https://github.com/swiftlang/swift-testing) (`@Test` /
`#expect`), which ships with the Swift toolchain. The generator only parses and
prints Swift source, so its tests run on Linux as well as macOS.

### Code coverage

```sh
scripts/export-coverage.sh
```

The script runs the generator's tests with coverage enabled, prints a per-file
summary, and writes `coverage.lcov` to the repository root. Coverage is
measured over `Sources/` only, and the paths in the report are relative to the
repository root.

CI uploads the macOS report to
[Codecov](https://codecov.io/gh/yysskk/kmp-swift-mockable), which comments on
pull requests with the resulting change. `codecov.yml` holds the thresholds:
the project total may drift by 1%, while new and changed lines are expected to
reach 80%. Pull requests from forks skip the upload, because they cannot read
the repository's Codecov token; run the script locally to check coverage on
those.

## Commit and PR conventions

- Use [Conventional Commits](https://www.conventionalcommits.org/) for commit
  and PR titles (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `ci:`, `docs:`).
- Keep pull requests focused and reviewable.
- Fill in the pull request template, including the testing and docs checklist.
