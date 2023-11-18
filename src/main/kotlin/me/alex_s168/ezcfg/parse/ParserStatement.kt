package me.alex_s168.ezcfg.parse

import me.alex_s168.ezcfg.*
import me.alex_s168.ezcfg.ast.ASTAssignment
import me.alex_s168.ezcfg.ast.ASTFunctionCall
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.ast.ASTVariableReference
import me.alex_s168.ezcfg.tokens.Token
import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ezcfg.tokens.TokenType
import me.alex_s168.ktlib.async.AsyncTask
import me.alex_s168.ktlib.async.concurrentMutableListOf
import me.alex_s168.ktlib.tree.MutableNode

/**
 * Parses a statement.
 *
 * @param inp The list of tokens to parse.
 * @param errorContext The error context to add errors to.
 * @return The ast node.
 */
internal fun parseStatement(
    inp: List<Token>,
    errorContext: ErrorContext,
    tasks: MutableCollection<AsyncTask>
): MutableNode<ASTValue> {
    val none = MutableNode<ASTValue>(
        null,
        concurrentMutableListOf(),
        null
    )

    if (inp.isEmpty()) {
        return none
    }

    var i = 0
    var token: Token = inp[i]

    fun consume(): Token? {
        i++
        if (i >= inp.size) {
            return null
        }
        token = inp[i]
        return inp[i - 1]
    }

    consume() ?: return none

    if (token.type != TokenType.IDENTIFIER) {
        errorContext.addError(token.location, "Expected identifier!")
        return none
    }

    val left = token

    consume() ?: return none

    when (token.type) {
        TokenType.PARENTHESES_OPEN -> {
            val children = concurrentMutableListOf<MutableNode<ASTValue>>()

            children += MutableNode(
                ASTVariableReference(
                    left.value,
                    left.location
                ),
                concurrentMutableListOf(),
                null
            )

            consume() ?: return none

            while (token.type != TokenType.PARENTHESES_CLOSE) {
                val (used, expr) = parseExpression(inp, i, errorContext, tasks)
                i += used
                if (i >= inp.size) {
                    errorContext.addError(token.location, "Missing closing parentheses!")
                    return none
                }
                token = inp[i]
                children += expr
            }

            consume()

            return MutableNode(
                ASTFunctionCall(
                    TokenLocation(
                        left.location.line,
                        left.location.column,
                        token.location.column - left.location.column,
                        left.location.root
                    )
                ),
                children,
                null
            )
        }
        TokenType.EQUALS -> {
            consume() ?: return none

            // ignores the amount of used tokens because it only uses as much as needed
            val (_, right) = parseExpression(inp, i, errorContext, tasks)

            return MutableNode(
                ASTAssignment(
                    TokenLocation(
                        left.location.line,
                        left.location.column,
                        (right.value?.loc?.column ?: left.location.column) - left.location.column,
                        left.location.root
                    )
                ),
                concurrentMutableListOf(
                    MutableNode(
                        ASTVariableReference(
                            left.value,
                            left.location
                        ),
                        concurrentMutableListOf(),
                        null
                    ),
                    right
                ),
                null
            )
        }
        else -> {
            errorContext.addError(left.location, "Unexpected token! [B]")
            return none
        }
    }
}
