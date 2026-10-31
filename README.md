# kmp-swift-mockable

Generate Swift mocks of Kotlin Multiplatform interfaces for iOS tests.

Annotate a Kotlin interface with `@SwiftMockable`, and the Kotlin build writes a Swift protocol that mirrors it. [swift-mockable](https://github.com/yysskk/swift-mockable) expands that protocol into a `<Name>Mock` usable wherever Kotlin expects the interface.

This project is under development and not yet released.

## License

MIT. See [LICENSE](LICENSE).
