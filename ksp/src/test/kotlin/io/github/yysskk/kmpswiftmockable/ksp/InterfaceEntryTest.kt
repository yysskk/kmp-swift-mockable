package io.github.yysskk.kmpswiftmockable.ksp

import kotlin.test.Test
import kotlin.test.assertEquals

class InterfaceEntryTest {
    @Test
    fun `entries are sorted, deduplicated, and escaped`() {
        val json = InterfaceEntry.listJson(
            listOf(
                InterfaceEntry("b.B", "B"),
                InterfaceEntry("a.A", "Quote\"Back\\slash\u0001"),
                InterfaceEntry("b.B", "B"),
            ),
        )

        assertEquals(
            """
            {"interfaces": [
                {"kotlinName": "a.A", "swiftName": "Quote\"Back\\slash\u0001"},
                {"kotlinName": "b.B", "swiftName": "B"}
            ]}

            """.trimIndent(),
            json,
        )
    }
}
