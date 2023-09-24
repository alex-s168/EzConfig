package me.alex_s168.ezcfg

import me.alex_s168.ktlib.tree.Node

class Variable(
    val name: String,
    val type: Type,
    val value: Node<ASTValue>,
) {
    var exported: Boolean = false
    var native: Boolean = false
}

fun Node<ASTFile>.findVariable(name: String): Variable? {
    // account for stuff like "x.y.z" and namespaces and object members.
    TODO()
}