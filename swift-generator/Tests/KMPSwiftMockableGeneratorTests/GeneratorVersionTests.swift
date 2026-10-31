import Testing

import KMPSwiftMockableGenerator

@Suite("Generator Version Tests")
struct GeneratorVersionTests {
    @Test("The version is a semantic version")
    func versionIsSemantic() {
        let components = GeneratorVersion.current.split(separator: ".")
        #expect(components.count == 3)
        #expect(components.allSatisfy { Int($0) != nil })
    }
}
