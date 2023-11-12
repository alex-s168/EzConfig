package me.alex_s168.ezcfg.i

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.ast.*
import me.alex_s168.ezcfg.check.generateASTFor
import me.alex_s168.ezcfg.check.resolve
import me.alex_s168.ezcfg.clear
import me.alex_s168.ezcfg.done
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ezcfg.tokens.RootTokenLocation
import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ktlib.async.await
import me.alex_s168.ktlib.async.concurrentMutableCollectionOf
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.MutableTree
import me.alex_s168.ktlib.tree.Node

/**
 * @param sources The sources to execute. Example source: [FileSource]
 * @param combineExceptions If this option is enabled, all exceptions will be caught and then thrown as one exception.
 */
@Throws(ConfigException::class)
@Suppress("UNCHECKED_CAST")
fun execute(
    sources: Iterable<Source>,
    functions: Map<String, NativeFunction> = mapOf(),
    combineExceptions: Boolean = false
) {
    val rootValue = ASTRoot(
        loc = TokenLocation(
            line = 0,
            column = 0,
            length = 0,
            code = "",
            rootLocation = RootTokenLocation(
                file = "ROOT"
            )
        )
    )

    val ast = MutableTree<ASTValue>(
        MutableNode(
            value = rootValue,
            children = concurrentMutableCollectionOf(),
            parent = null
        )
    )

    val tokenizingErrors = ErrorContext("tokenizing")
    val parserErrors = ErrorContext("parsing")
    val processingErrors = ErrorContext("processing")

    var hadException = false

    sources.forEach {
        generateASTFor(
            it.read(),
            it.path(),
            ast,
            tokenizingErrors,
            parserErrors,
            processingErrors
        ).await()

        try {
            tokenizingErrors.done()
        } catch (e: ConfigException) {
            if (combineExceptions) {
                hadException = true
            } else {
                throw e
            }
        }
        tokenizingErrors.clear()
        try {
        parserErrors.done()
        } catch (e: ConfigException) {
            if (combineExceptions) {
                hadException = true
            } else {
                throw e
            }
        }
        parserErrors.clear()
        try {
        processingErrors.done()
        } catch (e: ConfigException) {
            if (combineExceptions) {
                hadException = true
            } else {
                throw e
            }
        }
        processingErrors.clear()
    }

    if (hadException) {
        throw ConfigException("One or more exceptions occurred during stage 1!")
    }

    ast.updateParents()

    ast.root.children.forEach { f ->
        (f.value as ASTVariableHolding).variables.forEach {
            if (it.native && !it.exported) {
                if (combineExceptions) {
                    hadException = true
                } else {
                    throw ConfigException("Native function ${it.name} is not exported!")
                }
            }
            if (it.native && it.name !in functions) {
                if (combineExceptions) {
                    hadException = true
                } else {
                    throw ConfigException("Native function ${it.name} is not implemented!")
                }
            }
        }
    }

    if (hadException) {
        throw ConfigException("One or more exceptions occurred during stage 2!")
    }

    ast.root.children.forEach { fi ->
        fi.children.forEach {
            val f = it.value
            if (f is ASTFunctionCall) {
                var x: Node<ASTVariableReference> = it.children.first() as Node<ASTVariableReference>
                if (x.value!!.variable in listOf("function", "native", "export")) {
                    return@forEach
                }
                var name: String = x.value!!.variable
                while (name != "function") {
                    if (x.value!!.variable == "function") {
                        break
                    }
                    name = x.value!!.variable
                    x = x.resolve(name, fi as Node<ASTFile>, processingErrors)?.value
                            as? Node<ASTVariableReference>
                        ?: throw ConfigException("Unexpected error occurred! [gAST:r:1]")
                }
                functions[name]?.invoke(it.children.toList().getOrNull(1))
                    ?: throw ConfigException("Unexpected error occurred! [gAST:r:2]")
            }
        }
    }
}