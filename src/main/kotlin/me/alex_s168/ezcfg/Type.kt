package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ktlib.async.forEachAsyncConf
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.traverser.AsyncTreeTraverser

enum class TypeBase {
    STRING,
    NUMBER,
    ARRAY,
    FUNCTION,
    BLOCK,
}

// the idea is that every element in an array can be of a different type
data class Type(
    val type: TypeBase
)

fun Type.isCompatibleWith(other: Type): Boolean =
    this.type == other.type

fun MutableNode<ASTValue>.calculateTypes(errorContext: ErrorContext) {
    AsyncTreeTraverser.from(this) { node, traverser ->
        when (node.value) {
            is ASTString -> {
                node.value!!.type = Type(TypeBase.STRING)
            }
            is ASTNumber -> {
                node.value!!.type = Type(TypeBase.NUMBER)
            }
            is ASTArray -> {
                // manually first process the children
                node.children.forEachAsyncConf { child ->
                    traverser.process(child)
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
                val fileNode = node.topFile()
                    ?: throw ConfigException("Unexpected error occurred! [gAST:cT:1]")

                // when you do "register = function;" it declares variable register as function
                if (vn == "function") {
                    node.value!!.type = Type(TypeBase.FUNCTION)
                    return@from false
                }

                val variable = fileNode.findVariable(vn)
                if (variable == null) {
                    errorContext.addError(
                        loc = node.value!!.loc,
                        msg = "Variable $vn is not defined!"
                    )
                    return@from false
                }

                node.value!!.type = variable.type
            }
            else -> {
                return@from false
            }
        }
        return@from true
    }.traverse()
}