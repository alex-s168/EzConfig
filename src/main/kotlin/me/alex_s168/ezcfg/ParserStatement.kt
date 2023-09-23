package me.alex_s168.ezcfg

import me.alex_s168.ktlib.async.concurrentMutableListOf
import me.alex_s168.ktlib.tree.MutableNode

fun parseStatement(
    inp: List<Token>,
    errorContext: ErrorContext
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
                val (used, expr) = parseExpression(inp.subList(i, inp.size), errorContext)
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
                        left.location.code,
                        left.location.rootLocation
                    )
                ),
                children,
                null
            )
        }
        TokenType.EQUALS -> {
            consume() ?: return none

            val (used, right) = parseExpression(inp.subList(i, inp.size), errorContext)
            i += used
            if (i < inp.size) {
                errorContext.addError(inp[i].location, "Unexpected token! [C]")
            }

            return MutableNode(
                ASTAssignment(
                    TokenLocation(
                        left.location.line,
                        left.location.column,
                        (right.value?.loc?.column ?: left.location.column) - left.location.column,
                        left.location.code,
                        left.location.rootLocation
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
