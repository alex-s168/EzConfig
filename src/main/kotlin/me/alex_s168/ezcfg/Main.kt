package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.check.generateAST
import me.alex_s168.ezcfg.i.FileSource
import me.alex_s168.ezcfg.i.apply
import me.alex_s168.ezcfg.i.execute
import me.alex_s168.ktlib.tree.MutableTree
import kotlin.io.path.Path

/*
class Test2 {
    var f: Float = 0.0f
    lateinit var m: String

    override fun toString(): String = "Test2(f=$f, m=\"$m\")"
}

class Test {
    var c: Int = 0
    var d: Int = 0
    lateinit var y: Array<Int>
    lateinit var l: Test2

    override fun toString(): String = "Test(c=$c, d=$d, y=${y.contentToString()}, l=$l)"
}
 */

class RegistryElement {
    var name: String = ""

    override fun toString(): String = "RegistryElement(name=\"$name\")"
}

fun main() {
    execute(
        listOf(
            FileSource("run/test2.ezcfg")
        ),
        mapOf(
            "register" to { arg ->
                val r = arg!!.apply(RegistryElement())
                println("registered: $r")
            }
        )
    )

    // val path = Path("run/test2.ezcfg")
//
    // val inp = path.toFile().readText()
//
    // val ast: MutableTree<ASTValue> = generateAST(inp, path)
//
    // println(ast)

    // val x = ast.root.children.first().children.first().children.last() as Node<ASTBlock>
    // println(x.apply(Test()))
}