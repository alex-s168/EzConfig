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

    val inp = File("test.ezcfg").readText()

    val root = RootTokenLocation("test.ezcfg")

    val tokens = tokenize(inp, root, ErrorContext("tokenizing"))

    val ast: MutableTree<ASTValue>
    val parseTime = measureTime {
        //println(tokens.joinToString(separator = "\n") { it.toString() })

        val parserErrors = ErrorContext("parsing")

        ast = MutableTree(parseMain(tokens, parserErrors))

        ast.await()

        parserErrors.done()
    }.inWholeMilliseconds

    println("Parsing took $parseTime ms")

    ast.updateParents()

    println(ast)
}