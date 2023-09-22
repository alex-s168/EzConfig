import me.alex_s168.ktlib.tree.MutableTree
import java.io.File
import kotlin.time.measureTime

fun main(args: Array<String>) {
    val inp = File("test.ezcfg").readText()

    val root = RootTokenLocation("test.ezcfg")

    val tokens = tokenize(inp, root, ErrorContext("tokenizing"))

    val ast: MutableTree<ASTValue>
    val parseTime = measureTime {


        //println(tokens.joinToString(separator = "\n") { it.toString() })

        val parserErrors = ErrorContext("parsing")

        ast = parseMain(tokens, parserErrors)

        parserErrors.done()
    }.inWholeMilliseconds

    println("Parsing took $parseTime ms")

    ast.updateParents()

    println(ast)
}