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