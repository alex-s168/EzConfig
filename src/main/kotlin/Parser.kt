import me.alex_s168.ktlib.async.*
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.MutableTree

open class ASTValue(
    val name: String,
    val loc: TokenLocation
) {
    override fun toString(): String =
        name
}

class ASTFunctionCall(
    loc: TokenLocation
): ASTValue("call", loc)

class ASTVariableReference(
    val variable: String,
    loc: TokenLocation
): ASTValue("ref", loc) {
    override fun toString(): String =
        "$name: $variable"
}

class ASTString(
    val string: String,
    loc: TokenLocation
): ASTValue("string", loc) {
    override fun toString(): String =
        "$name: $string"
}

class ASTNumber(
    val number: Double,
    loc: TokenLocation
): ASTValue("number", loc) {
    override fun toString(): String =
        "$name: $number"
}

class ASTArray(
    loc: TokenLocation
): ASTValue("array", loc)

class ASTBlock(
    loc: TokenLocation
): ASTValue("block", loc)

class ASTAssignment(
    loc: TokenLocation
): ASTValue("assignment", loc)

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

    val none = Pair(0, MutableNode<ASTValue>(null, concurrentMutableCollectionOf(), null))

    when (token.type) {
        TokenType.IDENTIFIER -> {
            return Pair(
                i + 1,
                MutableNode(
                    ASTVariableReference(
                        token.value,
                        token.location
                    ),
                    concurrentMutableCollectionOf(),
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
                    concurrentMutableCollectionOf(),
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
                    concurrentMutableCollectionOf(),
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
                    m.root.children,
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

fun parseStatement(
    inp: List<Token>,
    errorContext: ErrorContext
): MutableNode<ASTValue> {
    val none = MutableNode<ASTValue>(
        null,
        concurrentMutableCollectionOf(),
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
                concurrentMutableCollectionOf(),
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
                errorContext.addError(inp[i].location, "Unexpected token!")
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
                concurrentMutableCollectionOf(
                    MutableNode(
                        ASTVariableReference(
                            left.value,
                            left.location
                        ),
                        concurrentMutableCollectionOf(),
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

fun parseMain(
    inp: List<Token>,
    errorContext: ErrorContext
): MutableTree<ASTValue> {
    var i = 0
    var token: Token

    fun consume(): Token? {
        i++
        if (i >= inp.size) return null
        token = inp[i]
        return inp[i - 1]
    }

    val tasks = mutableListOf<AsyncTask>()

    val statements = concurrentMutableListOf<MutableNode<ASTValue>>()
    val lock = Any()

    while (i < inp.size) {
        token = inp[i]
        if (token.type != TokenType.IDENTIFIER) {
            errorContext.addError(token.location, "Unexpected token! [A]")
        }

        val tokens = mutableListOf<Token>()
        tokens += token
        var ind = 0
        while (token.type != TokenType.SEMICOLON || ind > 0) {
            val tk = consume()
            if (tk == null) {
                errorContext.addError(token.location, "Missing semicolon!")
                break
            }
            if (tk.type == TokenType.CURLY_BRACE_OPEN) ind++
            else if (tk.type == TokenType.CURLY_BRACE_CLOSE) ind--
            tokens += tk
        }
        val where = statements.size
        statements += MutableNode(
            value = ASTValue("UNFINISHED TASK", token.location),
            children = mutableListOf(),
            parent = null
        )
        tasks += async {
            val ret = parseStatement(tokens, errorContext)
            synchronized(lock) {
                statements[where] = ret
            }
        }
        consume()
    }

    tasks.await()

    return MutableTree(
        MutableNode(
            value = null,
            children = statements,
            parent = null
        )
    )
}