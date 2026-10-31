# Contributing

Thanks for your interest in improving kmp-swift-mockable! This guide covers the
project layout, how to build and test, and the conventions we follow.

## Project layout

- `swift-generator/` — a Swift package with the generator that reads a
  framework's synthesized Swift interface and writes `@Mockable` mirror
  protocols.
  - `Sources/KMPSwiftMockableGenerator/` — the library: `ModuleInterface` reads
    protocols from a synthesized interface, and `MirrorGenerator` writes the
    mirror protocols.
  - `Sources/kmp-swift-mockable-generator/` — the command-line tool. Run
    `swift run kmp-swift-mockable-generator --help` for its options.
  - `Tests/KMPSwiftMockableGeneratorTests/` — tests, with interface fixtures
    shaped like real Kotlin/Native output in `Fixtures.swift`. The macOS-only
    tests write a header-only framework and run the interface synthesizer on it.
- `annotations/` — the `@SwiftMockable` annotation, a Kotlin Multiplatform
  library published for every Kotlin target.
- `ksp/` — the symbol processor that lists annotated interfaces in
  `kmp-swift-mockable/interfaces.json` for the generator. Its tests compile
  sources with [kotlin-compile-testing](https://github.com/ZacSweers/kotlin-compile-testing).
- `gradle-plugin/` — the Gradle plugin that wires the annotation, the
  processor, and a generation task for every Apple framework. Its tests apply
  it in-process with `ProjectBuilder`, and run whole builds with Gradle TestKit
  against empty stand-ins for the annotation and processor artifacts.
- `sample/` — a Kotlin Multiplatform project that applies the plugin from
  this build, and a Swift package (`sample/swift`) whose tests drive the
  sample's Kotlin code with the generated mocks.

Each Gradle project is named after the artifact it publishes
(`:kmp-swift-mockable-annotations`, ...), so that a build including this one,
such as the sample, uses the projects in place of the published artifacts.

On macOS, the plugin's jar bundles the generator, built by the
`buildGenerator` task as a stripped universal binary
(`swift build -c release --arch arm64 --arch x86_64`). A jar built on another
host has no generator, and builds using it must set
`kmpSwiftMockable.generatorExecutable`. The same property runs a generator
built from a local checkout while working on it.

## Building and testing

The Kotlin modules build with Gradle and JDK 17 or later:

```sh
./gradlew check
```

Apple targets compile only on macOS; on other hosts Gradle skips them.
`./gradlew koverXmlReport` writes the JVM modules' coverage to
`build/reports/kover/report.xml`, which CI uploads to Codecov next to the
generator's report.

The Swift generator builds with SwiftPM:

```sh
cd swift-generator
swift build
swift test
```

Tests use [swift-testing](https://github.com/swiftlang/swift-testing) (`@Test` /
`#expect`), which ships with the Swift toolchain. Apart from running
`swift-synthesize-interface`, which needs Xcode, the generator only parses and
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

### Running the sample

```sh
./gradlew -p sample assembleSharedDebugXCFramework
swift test --package-path sample/swift
```

The first command links the framework, which writes
`sample/swift/Tests/SharedTests/Generated/SharedMocks.swift`. CI compares that
file with `sample/SharedMocks.swift.expected`; when a change to the generator
changes the output on purpose, copy the new file over the snapshot.

## Keeping documentation in sync

When a change affects the generated output or user-facing behavior, update
these together so they don't drift:

- `README.md`
- the guides in `docs/`
- `llms.txt` and `llms-full.txt`, which restates the README and the guides in
  one file
- `CHANGELOG.md`

## Releasing

Pushing a tag `v<version>` runs `.github/workflows/release.yml` on macOS. It
checks that the tag matches `VERSION_NAME` in `gradle.properties`, runs the
tests, and publishes:

- `io.github.yysskk:kmp-swift-mockable-annotations` and
  `io.github.yysskk:kmp-swift-mockable-ksp` to Maven Central, signed;
- the `io.github.yysskk.kmp-swift-mockable` plugin, with the generator bundled
  in its jar, to the Gradle Plugin Portal;
- a GitHub release whose notes are the version's section of `CHANGELOG.md`.

The workflow needs these repository secrets:

| Secret | Value |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD` | A user token from the [Central Portal](https://central.sonatype.com/) account that owns the `io.github.yysskk` namespace. |
| `SIGNING_IN_MEMORY_KEY` | The ASCII-armored private GPG key that signs the artifacts (`gpg --armor --export-secret-keys <key id>`), with its public key on a keyserver. |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | The key's passphrase. |
| `GRADLE_PUBLISH_KEY`, `GRADLE_PUBLISH_SECRET` | An API key from the [Gradle Plugin Portal](https://plugins.gradle.org/) account. |

To release:

1. In a pull request, set `VERSION_NAME`, the generator's
   `GeneratorVersion.current`, and the header of
   `sample/SharedMocks.swift.expected` to the new version, and move the
   `Unreleased` entries of `CHANGELOG.md` under a heading for it.
2. Once it is merged, tag the merge commit and push the tag:

   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```

## Commit and PR conventions

- Use [Conventional Commits](https://www.conventionalcommits.org/) for commit
  and PR titles (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `ci:`, `docs:`).
- Keep pull requests focused and reviewable.
- Fill in the pull request template, including the testing and docs checklist.
