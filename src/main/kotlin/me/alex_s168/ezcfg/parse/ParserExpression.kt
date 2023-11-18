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

    fun expect(type: TokenType) {
        i++
        if (i >= inp.size) {
            errorContext.addError(token.location, "Unexpected end of file!")
            return
        }
        token = inp[i]
        if (token.type != type) {
            errorContext.addError(token.location, "Expected ${type.name}!")
        }
    }

    when (token.type) {
        TokenType.AND -> {
            val currLoc = token.location
            expect(TokenType.IDENTIFIER)
            val id = consume()!!
            val v = ASTString(
                id.value,
                TokenLocation(
                    currLoc.line,
                    currLoc.column,
                    currLoc.length + id.location.length,
                    currLoc.root
                )
            )
            return 2 to MutableNode(
                v,
                concurrentMutableListOf(),
                null
            )
        }
        TokenType.KEY_ENUM -> {
            expect(TokenType.CURLY_BRACE_OPEN)
            val startLoc = consume()?.location
            val children = mutableListOf<String>()
            while (token.type != TokenType.CURLY_BRACE_CLOSE) {
                if (token.type == TokenType.COMMA) {
                    val x = consume()
                    if (x == null) {
                        errorContext.addError(token.location, "Unexpected end of file!")
                        return none
                    }
                } else {
                    children += token.value
                    if (token.type != TokenType.IDENTIFIER) {
                        errorContext.addError(token.location, "Expected identifier!")
                        return none
                    }
                    val t = consume()
                    if (t == null) {
                        errorContext.addError(token.location, "Unexpected end of file!")
                        return none
                    }
                }
            }
            val v = ASTEnum(
                children,
                startLoc ?: token.location
            )
            return Pair(
                i + 1 - off,
                MutableNode(
                    v,
                    concurrentMutableListOf(),
                    null
                )
            )
        }
        TokenType.COLON -> {
            // typeof
            parseExpression(inp, i + 1, errorContext, tasks).let { (used, expr) ->
                val v = ASTTypeOf(
                    TokenLocation(
                        token.location.line,
                        token.location.column,
                        token.location.column - token.location.column,
                        token.location.root
                    )
                )
                return Pair(
                    used + 1,
                    MutableNode(
                        v,
                        concurrentMutableListOf(expr),
                        null
                    )
                )
            }
        }
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
                xtsk.asRunning().await()
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
                    token.location.root
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
                            token.location.root
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