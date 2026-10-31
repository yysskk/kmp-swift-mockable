import Foundation

/// Writes generated source to disk.
package enum GeneratedFile {
    /// Writes the contents to the file unless it already holds them, creating the
    /// directory if needed. Leaving an unchanged file alone keeps its modification
    /// date, so Xcode does not recompile the test target for nothing.
    ///
    /// - Returns: Whether the file was written.
    @discardableResult
    package static func write(_ contents: String, to url: URL) throws -> Bool {
        let data = Data(contents.utf8)
        if (try? Data(contentsOf: url)) == data {
            return false
        }
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: url, options: .atomic)
        return true
    }
}
