package me.alex_s168.ezcfg

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

internal fun getPath(pIn: String, fromIn: Path, root: ASTRoot): Path? {
    val p = pIn.substring(1, pIn.length - 1)
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