package me.alex_s168.ezcfg

import me.alex_s168.ktlib.async.concurrentMutableListOf
import me.alex_s168.ktlib.async.forEachAsync
import me.alex_s168.ktlib.tree.MutableNode

fun parseExpression(
    inp: List<Token>,
    errorContext: ErrorContext
): Pair<Int, MutableNode<ASTValue>> {
    // returns the number of tokens used and the expression and the ast node
    // handles expressions:
    // - string
    // - number (double)
    // - array (using square brackets)
    // - block (using curly braces)

    var i = 0
    var token: Token = inp[i]

    fun consume(): Token? {
        i++
        if (i >= inp.size) return null
        token = inp[i]
        return inp[i - 1]
    }

    val none = Pair(0, MutableNode<ASTValue>(null, concurrentMutableListOf(), null))

    when (token.type) {
        TokenType.IDENTIFIER -> {
            return Pair(
                i + 1,
                MutableNode(
                    ASTVariableReference(
                        token.value,
                        token.location
                    ),
                    concurrentMutableListOf(),
                    null
                )
            )
        }
        TokenType.STRING -> {
            return Pair(
                i + 1,
                MutableNode(
                    ASTString(
                        token.value,
                        token.location
                    ),
                    concurrentMutableListOf(),
                    null
                )
            )
        }
        TokenType.NUMBER -> {
            val num = token.value.toDoubleOrNull()
            if (num == null) {
                errorContext.addError(token.location, "Invalid number!")
                return none
            }
            return Pair(
                i + 1,
                MutableNode(
                    ASTNumber(
                        num,
                        token.location
                    ),
                    concurrentMutableListOf(),
                    null
                )
            )
        }
        TokenType.SQUARE_BRACE_OPEN -> {
            val children = concurrentMutableListOf<MutableNode<ASTValue>>()
            consume() ?: return none

            val tokens = mutableListOf<MutableList<Token>>()
            var ind = 0
            var tk = mutableListOf<Token>()
            while (token.type != TokenType.SQUARE_BRACE_CLOSE || ind > 0) {
                if (token.type == TokenType.SQUARE_BRACE_OPEN) ind++
                else if (token.type == TokenType.SQUARE_BRACE_CLOSE) ind--
                if (token.type == TokenType.COMMA && ind == 0) {
                    tokens += tk
                    tk = mutableListOf()
                } else {
                    tk += token
                }
                consume() ?: return none
            }
            tokens += tk

            tokens.forEachAsync { tks ->
                val (used, expr) = parseExpression(tks, errorContext)
                if (used != tks.size) {
                    errorContext.addError(tks[used].location, "Unexpected token!")
                }
                children += expr
            }

            val v = ASTArray(
                TokenLocation(
                    token.location.line,
                    token.location.column,
                    token.location.column - token.location.column,
                    token.location.code,
                    token.location.rootLocation
                )
            )
            return Pair(
                i + 1,
                MutableNode(
                    v,
                    children,
                    null
                )
            )
        }
        TokenType.CURLY_BRACE_OPEN -> {
            // uses parseMain as it is a block of statements
            val tokens = mutableListOf<Token>()
            var ind = 0
            while (token.type != TokenType.CURLY_BRACE_CLOSE || ind == 0) {
                consume() ?: return none
                if (token.type == TokenType.CURLY_BRACE_OPEN) ind++
                else if (token.type == TokenType.CURLY_BRACE_CLOSE) ind--
                tokens += token
            }
            val m = parseMain(tokens.dropLast(1), errorContext)
            consume()
            return Pair(
                i + 1,
                MutableNode(
                    ASTBlock(
                        TokenLocation(
                            token.location.line,
                            token.location.column,
                            token.location.column - token.location.column,
                            token.location.code,
                            token.location.rootLocation
                        )
                    ),
                    m.children,
                    null
                )
            )
        }
        else -> {
            errorContext.addError(token.location, "Unexpected token!")
            return none
        }
    }
}