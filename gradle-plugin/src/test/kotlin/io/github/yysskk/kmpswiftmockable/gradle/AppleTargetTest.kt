package io.github.yysskk.kmpswiftmockable.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppleTargetTest {
    @Test
    fun `simulator targets use a simulator triple and SDK`() {
        assertEquals(AppleTarget("arm64-apple-ios14.0-simulator", "iphonesimulator"), AppleTarget.forKonanTarget("ios_simulator_arm64"))
        assertEquals(AppleTarget("x86_64-apple-ios14.0-simulator", "iphonesimulator"), AppleTarget.forKonanTarget("ios_x64"))
        assertEquals(AppleTarget("arm64-apple-tvos14.0-simulator", "appletvsimulator"), AppleTarget.forKonanTarget("tvos_simulator_arm64"))
        assertEquals(AppleTarget("arm64-apple-watchos7.0-simulator", "watchsimulator"), AppleTarget.forKonanTarget("watchos_simulator_arm64"))
    }

    @Test
    fun `device targets use a device triple and SDK`() {
        assertEquals(AppleTarget("arm64-apple-ios14.0", "iphoneos"), AppleTarget.forKonanTarget("ios_arm64"))
        assertEquals(AppleTarget("arm64-apple-macos11.0", "macosx"), AppleTarget.forKonanTarget("macos_arm64"))
        assertEquals(AppleTarget("x86_64-apple-macos11.0", "macosx"), AppleTarget.forKonanTarget("macos_x64"))
        assertEquals(AppleTarget("arm64-apple-tvos14.0", "appletvos"), AppleTarget.forKonanTarget("tvos_arm64"))
        assertEquals(AppleTarget("arm64_32-apple-watchos7.0", "watchos"), AppleTarget.forKonanTarget("watchos_arm64"))
        assertEquals(AppleTarget("armv7k-apple-watchos7.0", "watchos"), AppleTarget.forKonanTarget("watchos_arm32"))
        assertEquals(AppleTarget("arm64-apple-watchos7.0", "watchos"), AppleTarget.forKonanTarget("watchos_device_arm64"))
    }

    @Test
    fun `non-Apple targets have no Apple target`() {
        assertNull(AppleTarget.forKonanTarget("linux_x64"))
        assertNull(AppleTarget.forKonanTarget("android_arm64"))
    }
}
