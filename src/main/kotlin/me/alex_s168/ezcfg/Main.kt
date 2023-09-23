package me.alex_s168.ezcfg

import me.alex_s168.ktlib.tree.MutableTree
import kotlin.io.path.Path
import kotlin.time.measureTime

internal class Dummy {
    fun m() {}
}

internal object ManualClassLoader {
    internal fun load() {
        for (i in 0..99999) {
            val dummy = Dummy()
            dummy.m()
        }
    }
}

fun main() {
    ManualClassLoader.load()

    val path = Path("run/categories.ezcfg")

    val inp = path.toFile().readText()

    val ast: MutableTree<ASTValue>
    val time = measureTime {
        ast = generateAST(inp, path)
    }.inWholeMilliseconds

    println("Generating and fixing the ast took: $time ms")

    println(ast)
}