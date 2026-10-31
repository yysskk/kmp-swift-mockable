package io.github.yysskk.kmpswiftmockable.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

/** The names Kotlin/Native gives declarations when it exports them to Objective-C and Swift. */
internal object SwiftNames {
    private const val OBJC_NAME = "kotlin.native.ObjCName"

    /**
     * The Swift name of an exported class or interface.
     *
     * `@ObjCName(swiftName = ...)` sets it, and `@ObjCName(name)` without a Swift name
     * sets both names. Otherwise a top-level declaration keeps its simple name, and a
     * nested one is prefixed with its outer declaration's Swift name: `Outer.Inner`
     * becomes `OuterInner`. Kotlin/Native can still rename a declaration whose name
     * collides with another; the generator reports names it cannot find.
     */
    fun exportedName(declaration: KSClassDeclaration): String {
        declaredName(declaration)?.let { return it }
        val outer = declaration.parentDeclaration as? KSClassDeclaration
        val simpleName = declaration.simpleName.asString()
        return if (outer == null) simpleName else exportedName(outer) + simpleName
    }

    private fun declaredName(declaration: KSClassDeclaration): String? {
        val annotation = declaration.annotations.firstOrNull { annotation ->
            annotation.shortName.asString() == "ObjCName" &&
                annotation.annotationType.resolve().declaration.qualifiedName?.asString() == OBJC_NAME
        } ?: return null
        val arguments = annotation.arguments.associate { it.name?.asString() to it.value as? String }
        return arguments["swiftName"]?.takeIf(String::isNotEmpty)
            ?: arguments["name"]?.takeIf(String::isNotEmpty)
    }
}
