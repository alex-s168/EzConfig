package me.alex_s168.ezcfg.tokens

data class RootTokenLocation(
    val file: String,
    val code: String
)

// TODO: refactor: move val code into RootTokenLocation
data class TokenLocation(
    val line: Int,
    val column: Int,
    val length: Int,
    val root: RootTokenLocation
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
    COLON,              // :
    AND,                // &

    KEY_ENUM,           // enum (keyword)
}
