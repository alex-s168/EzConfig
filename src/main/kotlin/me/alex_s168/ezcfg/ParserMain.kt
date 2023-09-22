package me.alex_s168.ezcfg

import me.alex_s168.ktlib.async.*
import me.alex_s168.ktlib.tree.MutableNode

fun parseMain(
    inp: List<Token>,
    errorContext: ErrorContext
): MutableNode<ASTValue> {
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

    return MutableNode(
        value = null,
        children = statements,
        parent = null,
        childrenFuture = tasks.createFuture { 0 }
    )
}