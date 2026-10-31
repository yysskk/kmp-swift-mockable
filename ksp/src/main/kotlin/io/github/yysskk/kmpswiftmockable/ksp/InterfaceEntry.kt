package io.github.yysskk.kmpswiftmockable.ksp

/** An annotated interface: its Kotlin name, and the name Kotlin/Native exports it under. */
internal data class InterfaceEntry(val kotlinName: String, val swiftName: String) {
    companion object {
        /**
         * The entries as the JSON document the Swift generator reads, sorted by Kotlin
         * name so that the file only changes when the set of interfaces does.
         */
        fun listJson(entries: Collection<InterfaceEntry>): String {
            val items = entries.distinct().sortedBy { it.kotlinName }.joinToString(",\n") { entry ->
                """    {"kotlinName": ${entry.kotlinName.jsonString()}, "swiftName": ${entry.swiftName.jsonString()}}"""
            }
            return if (items.isEmpty()) "{\"interfaces\": []}\n" else "{\"interfaces\": [\n$items\n]}\n"
        }

        private fun String.jsonString(): String = buildString {
            append('"')
            for (character in this@jsonString) {
                when {
                    character == '"' -> append("\\\"")
                    character == '\\' -> append("\\\\")
                    character < ' ' -> append("\\u%04x".format(character.code))
                    else -> append(character)
                }
            }
            append('"')
        }
    }
}
