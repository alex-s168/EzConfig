package me.alex_s168.ezcfg.check

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.addError
import me.alex_s168.ezcfg.ast.ASTFile
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ezcfg.getParentBlock
import me.alex_s168.ktlib.tree.Node

class Variable(
    val name: String,
    var type: Type?,
    val value: Node<ASTValue>,
) {
    var exported: Boolean = false
    var native: Boolean = false

    override fun toString(): String =
        "\"$name\" (${if (exported) "exported;" else ""}${if (native) "native;" else ""}): ${type ?: "?"}: $value"

}

fun <T: ASTValue> Node<T>.resolve(name: String, master: Node<ASTFile>, ctx: ErrorContext): Variable? =
    this.resolve(name.split('.'), 0, master, ctx)

// TODO: check for name collisions and warn
@Suppress("UNCHECKED_CAST")
fun <T: ASTValue> Node<T>.resolve(name: List<String>, off: Int, master: Node<ASTFile>, ctx: ErrorContext): Variable? {
    val curr = name[off]
    val left = name.size - off - 1
    if (curr == "parent") {
        val p = this.getParentBlock()
        if (p == null) {
            ctx.addError(
                this.value!!.loc,
                "Cannot get parent of root element!"
            )
            return null
        }
        if (left == 0) {
            return Variable(
                "<parent>",
                p.value!!.type ?: throw ConfigException("Unexpected error occurred! [gAST:r:1]"),
                p as Node<ASTValue>
            )
        }
        return p.resolve(name, off + 1, master, ctx)
    }
    if (curr == "top") {
        if (left == 0) {
            return Variable(
                "<top>",
                Type(TypeBase.FILE_MAIN),
                master as Node<ASTValue>
            )
        }
        return master.resolve(name, off + 1, master, ctx)
    }
    var parent = this.getParentBlock()
    while (parent != null) {
        parent.value!!.variables.forEach {
            if (it.name == curr) {
                if (left == 0) {
                    return it
                }
                return it.value.resolve(name, off + 1, master, ctx)
            }
        }
        val new = parent.getParentBlock()
        if (new == parent) {
            break
        }
        parent = new
    }

    return null
}