package me.alex_s168.ezcfg.tokens

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.addError

private val simpleTokens = mapOf(
    "enum" to TokenType.KEY_ENUM,

    "=" to TokenType.EQUALS,
    ";" to TokenType.SEMICOLON,
    "{" to TokenType.CURLY_BRACE_OPEN,
    "}" to TokenType.CURLY_BRACE_CLOSE,
    "[" to TokenType.SQUARE_BRACE_OPEN,
    "]" to TokenType.SQUARE_BRACE_CLOSE,
    "," to TokenType.COMMA,
    "(" to TokenType.PARENTHESES_OPEN,
    ")" to TokenType.PARENTHESES_CLOSE,
    ":" to TokenType.COLON,
    "&" to TokenType.AND
)

fun tokenize(
    inp: String,
    root: RootTokenLocation,
    errorContext: ErrorContext
): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    var line = 1
    var column = 1
    while (i < inp.length) {
        val ac = inp.substring(i)

        var found = false
        for (it in simpleTokens) {
            if  (ac.startsWith(it.key)) {
                tokens += Token(
                    it.value,
                    it.key,
                    TokenLocation(
                        line = line,
                        column = column,
                        length = it.key.length,
                        root = root
                    )
                )
                i += it.key.length
                column += it.key.length
                found = true
                break
            }
        }
        if (found) continue

        val c = inp[i]
        when {
            c == '#' -> {
                while (inp[i] != '\n') {
                    i++
                    column++
                }
            }
            c == '\n' -> {
                line++
                column = 1
                i++
            }
            c.isWhitespace() -> {
                i++
                column++
            }
            c == '"' -> {
                val start = i
                i++
                column++
                while (inp[i] != '"') {
                    i++
                    column++
                }
                val end = i
                tokens += Token(
                    TokenType.STRING,
                    inp.substring(start, end + 1),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = end - start + 1,
                        root = root
                    )
                )
                i++
                column++
            }
            isStartOfNum(c) -> {
                val start = i
                i++
                column++
                while (isPartOfNum(inp[i])) {
                    i++
                    column++
                }
                val end = i
                tokens += Token(
                    TokenType.NUMBER,
                    inp.substring(start, end),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = end - start + 1,
                        root = root
                    )
                )
            }
            isStartOfIdentifier(c) -> {
                val start = i
                i++
                column++
                while (isPartOfIdentifier(inp[i])) {
                    i++
                    column++
                }
                val end = i
                tokens += Token(
                    TokenType.IDENTIFIER,
                    inp.substring(start, end),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = end - start + 1,
                        root = root
                    )
                )
            }
            else -> {
                errorContext.addError(
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        root = root
                    ),
                    "Unexpected character '$c'"
                )
                i++
                column++
            }
        }
    }
    return tokens
}

private fun isStartOfIdentifier(c: Char): Boolean =
    c.isLetter()

private fun isPartOfIdentifier(c: Char): Boolean {
    if (isStartOfIdentifier(c)) {
        return true
    }
    if (c.isDigit()) {
        return true
    }
    if (c == '_') {
        return true
    }
    if (c == '.') {
        return true
    }
    return false
}

private fun isStartOfNum(c: Char): Boolean =
    c.isDigit() || c == '-'

private fun isPartOfNum(c: Char): Boolean =
    c.isDigit() || c == '.'