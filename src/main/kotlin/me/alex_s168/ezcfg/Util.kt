package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.ast.ASTFile
import me.alex_s168.ezcfg.ast.ASTRoot
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.ast.ASTVariableHolding
import me.alex_s168.ktlib.tree.Node
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute

fun <T> Node<T>.top(): Node<T> {
    var ret = this
    while (ret.parent != null) {
        ret = ret.parent!!
    }
    return ret
}

@Suppress("UNCHECKED_CAST")
fun <T: ASTValue> Node<T>.topFile(): Node<ASTFile>? {
    var ret: Node<ASTValue>? = this as Node<ASTValue>
    while (ret != null) {
        if (ret.value is ASTFile) {
            return ret as Node<ASTFile>
        }
        ret = ret.parent
    }
    return null
}

internal fun getPath(p: String, fromIn: Path, root: ASTRoot): Path? {
    val from = fromIn.absolute()
    return try {
        from.resolve("$p.ezcfg")
    } catch (e: InvalidPathException) {
        root.paths.forEach {
            if (it.fileName == Path("$p.ezcfg")) {
                return it
            }
        }
        null
    }
}

@Suppress("UNCHECKED_CAST")
fun <T: ASTValue> Node<T>.getParentBlock(): Node<ASTVariableHolding>? {
    if (this.value is ASTVariableHolding) {
        return this as Node<ASTVariableHolding>
    }
    var ret: Node<ASTValue>? = this as Node<ASTValue>
    while (ret != null) {
        if (ret.value is ASTVariableHolding) {
            return ret as Node<ASTVariableHolding>
        }
        ret = ret.parent
    }
    return null
}