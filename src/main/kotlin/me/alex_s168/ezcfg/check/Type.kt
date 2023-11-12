package me.alex_s168.ezcfg.check

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.addError
import me.alex_s168.ezcfg.ast.*
import me.alex_s168.ktlib.async.forEachAsyncConf
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.Node
import me.alex_s168.ktlib.tree.traverser.AsyncTreeTraverser

enum class TypeBase {
    STRING,
    NUMBER,
    ARRAY,
    FUNCTION,
    BLOCK,

    FILE_MAIN
}

// the idea is that every element in an array can be of a different type
data class Type(
    val type: TypeBase
)

fun Type.isCompatibleWith(other: Type): Boolean =
    this.type == other.type

@Suppress("UNCHECKED_CAST")
fun MutableNode<ASTValue>.calculateTypes(errorContext: ErrorContext, fileNode: Node<ASTValue>) {
    AsyncTreeTraverser.from(this) { node, traverser ->
        if (node.value is ASTVariableHolding) {
            (node.value!! as ASTVariableHolding).variables.forEachAsyncConf { variable ->
                traverser.process(variable.value)
            }
        }
        when (node.value) {
            is ASTString -> {
                node.value!!.type = Type(TypeBase.STRING)
            }
            is ASTNumber -> {
                node.value!!.type = Type(TypeBase.NUMBER)
            }
            is ASTArray -> {
                // manually first process the children synchronously
                node.children.forEach {
                    traverser.process(it)
                }
                node.value!!.type = Type(
                    TypeBase.ARRAY
                )
                // do not process the children again
                return@from false
            }
            is ASTFunctionCall -> {
                return@from false
            }
            is ASTBlock -> {
                node.value!!.type = Type(TypeBase.BLOCK)
            }
            is ASTVariableReference -> {
                val vn = (node.value!! as ASTVariableReference).variable

                // when you do "register = function;" it declares variable register as function
                if (vn == "function") {
                    node.value!!.type = Type(TypeBase.FUNCTION)
                    return@from false
                }

                val variable = node.resolve(vn, fileNode as Node<ASTFile>, errorContext)
                if (variable == null) {
                    errorContext.addError(
                        loc = node.value!!.loc,
                        msg = "Variable $vn is not defined!"
                    )
                    return@from false
                }

                node.value!!.type = variable.type
                if (node is MutableNode<*>) {
                    node as MutableNode<ASTValue>
                    node.value = variable.value.value
                    node.children.clear()
                    node.children += variable.value.children as Collection<MutableNode<ASTValue>>
                }
            }
            is ASTAssignment -> {
                traverser.process(node.children.last())
                return@from false
            }
            else -> {
                return@from false
            }
        }
        return@from true
    }.traverse()
}