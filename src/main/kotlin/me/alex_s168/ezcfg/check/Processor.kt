package me.alex_s168.ezcfg.check

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.tokens.RootTokenLocation
import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ezcfg.ast.ASTRoot
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.done
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ktlib.async.*
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.MutableTree
import java.nio.file.Path

/**
 * Generates the AST for a given text and file path.
 * @throws ConfigException
 */
fun generateAST(input: String, path: Path): MutableTree<ASTValue> {
    val rootValue = ASTRoot(
        loc = TokenLocation(
            line = 0,
            column = 0,
            length = 0,
            root = RootTokenLocation(
                file = "ROOT",
                code = input,
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

    generateASTFor(
        input,
        path,
        ast,
        tokenizingErrors,
        parserErrors,
        processingErrors
    ).await()

    tokenizingErrors.done()
    parserErrors.done()
    processingErrors.done()

    ast.updateParents()

    return ast
}