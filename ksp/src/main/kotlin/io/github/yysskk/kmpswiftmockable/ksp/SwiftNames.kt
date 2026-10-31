package io.github.yysskk.kmpswiftmockable.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

/** The names Kotlin/Native gives declarations when it exports them to Objective-C and Swift. */
internal object SwiftNames {
    private const val OBJC_NAME = "kotlin.native.ObjCName"

    /**
     * The Swift name of an exported class or interface.
     *
     * A top-level declaration keeps its simple name, and a nested one is prefixed with
     * its outer declaration's Swift name: `Outer.Inner` becomes `OuterInner`.
     * `@ObjCName(swiftName = ...)`, or `@ObjCName(name)` without a Swift name, replaces
     * the simple name but keeps the prefix, unless `exact = true` makes it the whole
     * name. Kotlin/Native can still rename a declaration whose name collides with
     * another; the generator reports names it cannot find.
     */
    fun exportedName(declaration: KSClassDeclaration): String {
        val objCName = objCName(declaration)
        val name = objCName?.name ?: declaration.simpleName.asString()
        if (objCName?.exact == true) return name
        val outer = declaration.parentDeclaration as? KSClassDeclaration ?: return name
        return exportedName(outer) + name
    }

    private class ObjCName(val name: String?, val exact: Boolean)

    private fun objCName(declaration: KSClassDeclaration): ObjCName? {
        val annotation = declaration.annotations.firstOrNull { annotation ->
            annotation.shortName.asString() == "ObjCName" &&
                annotation.annotationType.resolve().declaration.qualifiedName?.asString() == OBJC_NAME
        } ?: return null
        val arguments = annotation.arguments.associate { it.name?.asString() to it.value }
        val name = (arguments["swiftName"] as? String)?.takeIf(String::isNotEmpty)
            ?: (arguments["name"] as? String)?.takeIf(String::isNotEmpty)
        return ObjCName(name, exact = arguments["exact"] == true)
    }
}
