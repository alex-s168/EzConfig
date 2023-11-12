package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.ast.ASTBlock
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.check.generateAST
import me.alex_s168.ktlib.tree.MutableTree
import me.alex_s168.ktlib.tree.Node
import kotlin.io.path.Path
import kotlin.time.measureTime

class Test2 {
    var f: Float = 0.0f

    override fun toString(): String = "Test2(f=$f)"
}

class Test {
    var c: Int = 0
    var d: Int = 0
    lateinit var y: Array<Int>
    lateinit var l: Test2

    override fun toString(): String = "Test(c=$c, d=$d, y=${y.contentToString()}, l=$l)"
}

fun main() {
    val path = Path("run/test.ezcfg")

    val inp = path.toFile().readText()

    val ast: MutableTree<ASTValue>
    val time = measureTime {
        ast = generateAST(inp, path)
    }.inWholeMilliseconds

    val x = ast.root.children.first().children.first().children.last() as Node<ASTBlock>
    println(x.apply(Test()))
}