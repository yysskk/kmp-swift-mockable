import Foundation
import Testing

@testable import KMPSwiftMockableGenerator

@Suite("Interface List Tests")
struct InterfaceListTests {
    @Test("A list written by the symbol processor is read")
    func readsList() throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID()).json")
        defer { try? FileManager.default.removeItem(at: url) }
        try Data("""
            {"interfaces": [{"kotlinName": "com.example.UserRepository", "swiftName": "UserRepository"}]}
            """.utf8).write(to: url)

        let list = try InterfaceList(contentsOf: url)
        #expect(list == InterfaceList(interfaces: [
            InterfaceList.Entry(kotlinName: "com.example.UserRepository", swiftName: "UserRepository"),
        ]))
    }

    @Test("A malformed list is an error")
    func malformedList() throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("\(UUID()).json")
        defer { try? FileManager.default.removeItem(at: url) }
        try Data(#"{"interfaces": [{"kotlinName": "A"}]}"#.utf8).write(to: url)

        #expect(throws: DecodingError.self) { try InterfaceList(contentsOf: url) }
    }
}
