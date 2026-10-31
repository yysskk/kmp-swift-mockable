package io.github.yysskk.kmpswiftmockable.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

/** Creates [SwiftMockableProcessor]. KSP finds this provider through `META-INF/services`. */
public class SwiftMockableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        SwiftMockableProcessor(environment.codeGenerator, environment.logger)
}

/**
 * Lists the interfaces annotated with `@SwiftMockable` in [INTERFACE_LIST_PATH], for the
 * Swift generator to mirror. It generates no code.
 *
 * The list is an aggregating output: it depends on every annotated file, so KSP rewrites
 * it whenever an annotation is added or removed.
 */
public class SwiftMockableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val interfaces = mutableListOf<InterfaceEntry>()
    private val sources = mutableSetOf<KSFile>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (symbol in resolver.getSymbolsWithAnnotation(ANNOTATION_NAME)) {
            if (symbol !is KSClassDeclaration || symbol.classKind != ClassKind.INTERFACE) {
                logger.error("@SwiftMockable can only be applied to interfaces.", symbol)
                continue
            }
            interfaces += InterfaceEntry(
                kotlinName = symbol.qualifiedName?.asString() ?: symbol.simpleName.asString(),
                swiftName = SwiftNames.exportedName(symbol),
            )
            symbol.containingFile?.let(sources::add)
        }
        return emptyList()
    }

    override fun finish() {
        val dependencies = Dependencies(aggregating = true, *sources.toTypedArray())
        codeGenerator.createNewFileByPath(dependencies, INTERFACE_LIST_PATH, "json").use { stream ->
            stream.write(InterfaceEntry.listJson(interfaces).toByteArray())
        }
    }

    public companion object {
        /** The annotation's fully qualified name. */
        public const val ANNOTATION_NAME: String = "io.github.yysskk.kmpswiftmockable.SwiftMockable"

        /** The list's path in KSP's resource output, without the `.json` extension. */
        public const val INTERFACE_LIST_PATH: String = "kmp-swift-mockable/interfaces"
    }
}
