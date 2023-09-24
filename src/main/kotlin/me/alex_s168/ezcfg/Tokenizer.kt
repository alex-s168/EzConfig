package me.alex_s168.ezcfg

data class RootTokenLocation(
    val file: String
)

// TODO: refactor: move val code into RootTokenLocation
data class TokenLocation(
    val line: Int,
    val column: Int,
    val length: Int,
    val code: String,
    val rootLocation: RootTokenLocation
)

data class Token(
    val type: TokenType,
    val value: String,
    val location: TokenLocation
) {

    override fun toString(): String =
        "Token(type=$type, value=\"$value\")"

}

enum class TokenType {
    IDENTIFIER,         // (start with letter, can contain letters, numbers, underscores, and dots)
    EQUALS,             // = (assignment)
    SEMICOLON,          // ; (statement terminator)
    STRING,             // "string" (double-quoted string literal)
    NUMBER,             // 123.456 (number literal (double))
    CURLY_BRACE_OPEN,   // { (starts a block of statements)
    CURLY_BRACE_CLOSE,  // } (ends a block of statements)
    SQUARE_BRACE_OPEN,  // [ (starts an array literal)
    SQUARE_BRACE_CLOSE, // ] (ends an array literal)
    COMMA,              // , (separates elements in an array literal)
    PARENTHESES_OPEN,   // ( (starts the arguments of a function call)
    PARENTHESES_CLOSE,  // ) (ends the arguments of a function call)
}

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
            c == '=' -> {
                tokens += Token(
                    TokenType.EQUALS,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == ';' -> {
                tokens += Token(
                    TokenType.SEMICOLON,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == '{' -> {
                tokens += Token(
                    TokenType.CURLY_BRACE_OPEN,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == '}' -> {
                tokens += Token(
                    TokenType.CURLY_BRACE_CLOSE,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == '[' -> {
                tokens += Token(
                    TokenType.SQUARE_BRACE_OPEN,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == ']' -> {
                tokens += Token(
                    TokenType.SQUARE_BRACE_CLOSE,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == ',' -> {
                tokens += Token(
                    TokenType.COMMA,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == '(' -> {
                tokens += Token(
                    TokenType.PARENTHESES_OPEN,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
                i++
                column++
            }
            c == ')' -> {
                tokens += Token(
                    TokenType.PARENTHESES_CLOSE,
                    c.toString(),
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
                    )
                )
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
                        code = inp,
                        rootLocation = root
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
                        code = inp,
                        rootLocation = root
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
                        code = inp,
                        rootLocation = root
                    )
                )
            }
            else -> {
                errorContext.addError(
                    TokenLocation(
                        line = line,
                        column = column,
                        length = 1,
                        code = inp,
                        rootLocation = root
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