package me.alex_s168.ezcfg

import me.alex_s168.ktlib.any.println
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.MutableTree
import java.io.File
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

    val inp = File("run/a10a.ezcfg").readText()

    val root = RootTokenLocation("run/a10a.ezcfg")

    val tokens = tokenize(inp, root, ErrorContext("tokenizing"))

    val parserErrors = ErrorContext("parsing")

    val ast: MutableTree<ASTValue>
    val parseTime = measureTime {
        //println(tokens.joinToString(separator = "\n") { it.toString() })

        ast = MutableTree(parseMain(tokens, parserErrors))

        ast.await()
    }.inWholeMilliseconds

    parserErrors.done()

    println("Parsing took $parseTime ms")

    ast.updateParents()

    println(ast)
}