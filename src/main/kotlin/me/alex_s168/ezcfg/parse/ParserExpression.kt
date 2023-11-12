package me.alex_s168.ezcfg.parse

import me.alex_s168.ezcfg.*
import me.alex_s168.ezcfg.ast.*
import me.alex_s168.ezcfg.tokens.Token
import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ezcfg.tokens.TokenType
import me.alex_s168.ktlib.async.*
import me.alex_s168.ktlib.tree.MutableNode

/**
 * Parses an expression.
 * @param inp The list of tokens to parse.
 * @param off The offset to start parsing at.
 * @param errorContext The error context to add errors to.
 * @return The number of tokens used and the expression and the ast node.
 */
internal fun parseExpression(
    inp: List<Token>,
    off: Int,
    errorContext: ErrorContext,
    tasks: MutableCollection<AsyncTask>
): Pair<Int, MutableNode<ASTValue>> {
    // returns the number of tokens used and the expression and the ast node
    // handles expressions:
    // - string
    // - number (double)
    // - array (using square brackets)
    // - block (using curly braces)

    val none = Pair(0, MutableNode<ASTValue>(null, concurrentMutableListOf(), null))

    var i = off

    if (i >= inp.size) {
        return none
    }

    var token: Token = inp[i]

    fun consume(): Token? {
        i++
        if (i >= inp.size) return null
        token = inp[i]
        return inp[i - 1]
    }

    when (token.type) {
        TokenType.IDENTIFIER -> {
            return Pair(
                1,
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
                1,
                MutableNode(
                    ASTString(
                        token.value.substring(1, token.value.length - 1),
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
                1,
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
            val children = mutableListOf<MutableNode<ASTValue>>()
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

            val xtsk = concurrentMutableCollectionOf<AsyncTask>()
            tokens.forEach { tks ->
                xtsk.clear()
                val (used, expr) = parseExpression(tks, 0, errorContext, xtsk)
                xtsk.await()
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
                ),
                children.toMutableList()
            )
            return Pair(
                i + 1 - off,
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
            while (token.type != TokenType.CURLY_BRACE_CLOSE || ind >= 0) {
                consume() ?: return none
                if (token.type == TokenType.CURLY_BRACE_OPEN) ind++
                else if (token.type == TokenType.CURLY_BRACE_CLOSE) ind--
                tokens += token
            }
            val m = parseMain(tokens.dropLast(1), errorContext, tasks)
            return Pair(
                i + 1 - off,
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