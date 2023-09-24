package me.alex_s168.ezcfg

import me.alex_s168.ktlib.async.concurrentMutableCollectionOf
import java.nio.file.Path

open class ASTValue(
    val nodeTypeName: String,
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

class ASTFile(
    loc: TokenLocation,
    val path: Path
): ASTValue("file", loc) {

    val variables = concurrentMutableCollectionOf<Variable>()
    val importedNamespaces = concurrentMutableCollectionOf<Pair<String, Path>>()

    override fun toString(): String =
        "$nodeTypeName: $path"
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
        "$nodeTypeName: $string"
}

class ASTNumber(
    val number: Double,
    loc: TokenLocation
): ASTValue("number", loc) {
    override fun toString(): String =
        "$nodeTypeName: $number"
}

class ASTArray(
    loc: TokenLocation
): ASTValue("array", loc)

class ASTBlock(
    loc: TokenLocation
): ASTValue("block", loc)

class ASTAssignment(
    loc: TokenLocation
): ASTValue("assignment", loc)