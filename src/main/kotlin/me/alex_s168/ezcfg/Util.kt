package me.alex_s168.ezcfg

import me.alex_s168.ktlib.tree.Node

fun <T> Node<T>.top(): Node<T> {
    var ret = this
    while (ret.parent != null) {
        ret = ret.parent!!
    }
    return ret
}

@Suppress("UNCHECKED_CAST")
fun Node<ASTValue>.topFile(): Node<ASTFile>? {
    var ret: Node<ASTValue>? = this
    while (ret != null) {
        if (ret.value is ASTFile) {
            return ret as Node<ASTFile>
        }
        ret = ret.parent
    }
    return null
}