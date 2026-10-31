import Foundation
import Testing

@testable import KMPSwiftMockableGenerator

@Suite("Generated File Tests")
struct GeneratedFileTests {
    private let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)

    @Test("A new file is written, creating its directory")
    func writesNewFile() throws {
        defer { try? FileManager.default.removeItem(at: directory) }
        let url = directory.appendingPathComponent("Nested/SharedMocks.swift")

        #expect(try GeneratedFile.write("a", to: url))
        #expect(try String(contentsOf: url, encoding: .utf8) == "a")
    }

    @Test("A file that already holds the contents is left alone")
    func skipsUnchangedFile() throws {
        defer { try? FileManager.default.removeItem(at: directory) }
        let url = directory.appendingPathComponent("SharedMocks.swift")
        try GeneratedFile.write("a", to: url)

        #expect(try !GeneratedFile.write("a", to: url))
        #expect(try GeneratedFile.write("b", to: url))
        #expect(try String(contentsOf: url, encoding: .utf8) == "b")
    }
}
