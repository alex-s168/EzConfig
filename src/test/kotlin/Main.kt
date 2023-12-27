import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.addError
import me.alex_s168.ezcfg.ast.ASTValue
import me.alex_s168.ezcfg.check.generateAST
import me.alex_s168.ezcfg.i.*
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

/*
class RegistryElement {
    var name: String = ""

    override fun toString(): String = "RegistryElement(name=\"$name\")"
}
*/

/*
enum class TestEnum {
    a, b, c
}

class Data {
    lateinit var v: TestEnum

    override fun toString(): String = "Data(v=$v)"
}
 */

/*
class Vec3f: SpecialSerializable<Vec3f> {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var z: Float = 0.0f

    override fun deserialize(element: Element, errorContext: ErrorContext) {
        val arr = element.apply(floatArrayOf(), errorContext)
        if (arr.size != 3) {
            errorContext.addError(element, "Expected array of size 3, got ${arr.size}")
            return
        }

        x = arr[0]
        y = arr[1]
        z = arr[2]
    }

    override fun toString(): String = "Vec3f(x=$x, y=$y, z=$z)"
}

class Data {
    lateinit var vec: Vec3f

    override fun toString(): String = "Data(vec=$vec)"
}

 */

data class Vec3(
    var x: Float,
    var y: Float,
    var z: Float
)

class Vec3Serializer: SerializationFunction<Vec3> {
    override fun deserialize(element: Element, errorContext: ErrorContext): Vec3 {
        val arr = element.apply(floatArrayOf(), errorContext)
        if (arr.size != 3) {
            errorContext.addError(element, "Expected array of size 3, got ${arr.size}")
            return Vec3(0.0f, 0.0f, 0.0f)
        }

        return Vec3(arr[0], arr[1], arr[2])
    }
}

class Data {
    @SerializationRules<Vec3>(function = Vec3Serializer::class)
    lateinit var vec: Vec3

    override fun toString(): String = "Data(vec=$vec)"
}

fun main() {
    execute(
        listOf(
            FileSource("run/specialread.ezcfg")
        ),
        mapOf(
            "myFun" to { arg, ctx ->
                val r = arg!!.apply(Data(), ctx)
                println("data: $r")
            }
        )
    )

    /*
    execute(
        listOf(
            FileSource("run/enums.ezcfg")
        ),
        mapOf(
            "register" to { arg, ctx ->
                val r = arg!!.apply(Data(), ctx)
                println("registered: $r")
            }
        )
    )
     */

    // val path = Path("run/enums.ezcfg")
    // val inp = path.toFile().readText()
    // val ast = generateAST(inp, path)
    // println(ast)

    // execute(
    //     listOf(
    //         FileSource("run/test2.ezcfg")
    //     ),
    //     mapOf(
    //         "register" to { arg ->
    //             val r = arg!!.apply(RegistryElement())
    //             println("registered: $r")
    //         }
    //     )
    // )

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