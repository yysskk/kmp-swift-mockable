# Security Policy

## Supported Versions

Security fixes are released for the latest published version of
kmp-swift-mockable. Please upgrade to the most recent release before reporting
an issue.

## Reporting a Vulnerability

Please do not report security vulnerabilities through public GitHub issues.

Instead, report them privately through GitHub's
[private vulnerability reporting](https://github.com/yysskk/kmp-swift-mockable/security/advisories/new).
This lets us discuss and address the issue before it is publicly disclosed.

When reporting, please include:

- A description of the vulnerability and its impact.
- Steps to reproduce, or a proof of concept.
- The affected version(s).

We will acknowledge your report as soon as we can and keep you informed of the
progress toward a fix.

## Scope

kmp-swift-mockable is a build-time code generator for tests: it runs during the
Kotlin build on a developer's or CI machine and writes Swift source for test
targets. Reports that are most relevant include issues in the generator or the
Gradle plugin that could lead to unexpected code generation, to running an
unintended executable, or to writing files outside the configured output
directory.
