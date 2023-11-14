package me.alex_s168.ezcfg.ast

import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ezcfg.check.Type
import me.alex_s168.ezcfg.check.TypeBase
import me.alex_s168.ezcfg.check.Variable
import me.alex_s168.ktlib.async.concurrentMutableCollectionOf
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.Node
import java.nio.file.Path

open class ASTValue(
    var nodeTypeName: String,
    val loc: TokenLocation
) {

    var type: Type? = null

    override fun toString(): String =
        nodeTypeName
}

class ASTRoot(
    loc: TokenLocation
): ASTValue("root", loc) {
    val imports = concurrentMutableCollectionOf<Path>()

    // constant paths
    val paths = concurrentMutableCollectionOf<Path>()
}

abstract class ASTVariableHolding(
    name: String,
    loc: TokenLocation
): ASTValue(name, loc) {
    open val variables = concurrentMutableCollectionOf<Variable>()

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("$nodeTypeName:\n")
        for (variable in variables) {
            sb.append(" - $variable\n")
        }
        return sb.toString()
    }
}

class ASTFile(
    loc: TokenLocation,
    val path: Path
): ASTVariableHolding("file", loc) {
    val importedNamespaces = concurrentMutableCollectionOf<Pair<String, Path>>()

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("$nodeTypeName: $path\n")
        sb.append(" variables:\n")
        for (variable in variables) {
            sb.append(" - $variable\n")
        }
        sb.append(" imported namespaces:\n")
        for (importedNamespace in importedNamespaces) {
            sb.append(" - $importedNamespace\n")
        }
        return sb.toString()
    }
}

class ASTFunctionCall(
    loc: TokenLocation
): ASTValue("call", loc)

class ASTVariableReference(
    val variable: String,
    loc: TokenLocation
): ASTValue("ref", loc) {
    override fun toString(): String =
        "$nodeTypeName: $variable"
}

class ASTString(
    val string: String,
    loc: TokenLocation
): ASTValue("string", loc) {
    override fun toString(): String =
        "$nodeTypeName: \"$string\""
}

class ASTNumber(
    val number: Double,
    loc: TokenLocation
): ASTValue("number", loc) {
    override fun toString(): String =
        "$nodeTypeName: $number"
}

class ASTTypeOf(
    loc: TokenLocation
): ASTValue("typeof", loc)

class ASTTypeLit(
    val value: Type,
    loc: TokenLocation
): ASTValue("typelit", loc) {
    override fun toString(): String =
        "$nodeTypeName: $value"
}

class ASTEnum(
    val members: List<String>,
    loc: TokenLocation
): ASTVariableHolding("enum", loc) {
    override fun toString(): String =
        "$nodeTypeName: $members"

    override fun equals(other: Any?): Boolean {
        if (other !is ASTEnum) return false
        return other.members == members
    }

    override fun hashCode(): Int {
        return members.hashCode() * 31
    }

    override val variables: MutableCollection<Variable>
        get() {
            return members.mapTo(concurrentMutableCollectionOf()) {
                val v = Variable(
                    it,
                    Type(TypeBase.ENUM_MEMBER),
                    MutableNode(
                        ASTEnumValue(
                            it,
                            this,
                            loc
                        ),
                        concurrentMutableCollectionOf(),
                        null
                    )
                )
                v.exported = true
                v
            }
        }
}

class ASTEnumValue(
    val value: String,
    val enum: ASTEnum,
    loc: TokenLocation
): ASTValue("enumval", loc) {
    override fun toString(): String =
        "$nodeTypeName: $value"
}

class ASTArray(
    loc: TokenLocation,
    val content: MutableList<MutableNode
    <ASTValue>>
): ASTValue("array", loc)

class ASTBlock(
    loc: TokenLocation
): ASTVariableHolding("block", loc)

class ASTAssignment(
    loc: TokenLocation
): ASTValue("assignment", loc)