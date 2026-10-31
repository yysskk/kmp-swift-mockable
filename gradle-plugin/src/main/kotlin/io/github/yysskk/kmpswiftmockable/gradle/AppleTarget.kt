package io.github.yysskk.kmpswiftmockable.gradle

/**
 * What the Swift interface synthesizer needs to know about a Kotlin/Native Apple target:
 * the triple a framework for it is compiled for, and the SDK it is compiled against.
 */
internal data class AppleTarget(val triple: String, val sdk: String) {
    internal companion object {
        // Deployment targets at or below Kotlin/Native's own minimums, which every
        // framework satisfies; the synthesizer only needs a valid triple.
        private const val IOS = "ios14.0"
        private const val MACOS = "macos11.0"
        private const val TVOS = "tvos14.0"
        private const val WATCHOS = "watchos7.0"

        private val targets = mapOf(
            "ios_arm64" to AppleTarget("arm64-apple-$IOS", "iphoneos"),
            "ios_x64" to AppleTarget("x86_64-apple-$IOS-simulator", "iphonesimulator"),
            "ios_simulator_arm64" to AppleTarget("arm64-apple-$IOS-simulator", "iphonesimulator"),
            "macos_arm64" to AppleTarget("arm64-apple-$MACOS", "macosx"),
            "macos_x64" to AppleTarget("x86_64-apple-$MACOS", "macosx"),
            "tvos_arm64" to AppleTarget("arm64-apple-$TVOS", "appletvos"),
            "tvos_x64" to AppleTarget("x86_64-apple-$TVOS-simulator", "appletvsimulator"),
            "tvos_simulator_arm64" to AppleTarget("arm64-apple-$TVOS-simulator", "appletvsimulator"),
            "watchos_arm32" to AppleTarget("armv7k-apple-$WATCHOS", "watchos"),
            "watchos_arm64" to AppleTarget("arm64_32-apple-$WATCHOS", "watchos"),
            "watchos_device_arm64" to AppleTarget("arm64-apple-$WATCHOS", "watchos"),
            "watchos_x64" to AppleTarget("x86_64-apple-$WATCHOS-simulator", "watchsimulator"),
            "watchos_simulator_arm64" to AppleTarget("arm64-apple-$WATCHOS-simulator", "watchsimulator"),
        )

        /** The Apple target for a Kotlin/Native target name such as `ios_simulator_arm64`, or `null`. */
        fun forKonanTarget(name: String): AppleTarget? = targets[name]
    }
}
