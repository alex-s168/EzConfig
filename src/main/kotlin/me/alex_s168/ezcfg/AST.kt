package me.alex_s168.ezcfg

open class ASTValue(
    val name: String,
    val loc: TokenLocation
) {

    val type: Type? = null

    override fun toString(): String =
        name
}

class ASTFunctionCall(
    loc: TokenLocation
): ASTValue("call", loc)

class ASTVariableReference(
    val variable: String,
    loc: TokenLocation
): ASTValue("ref", loc) {
    override fun toString(): String =
        "$name: $variable"
}

class ASTString(
    val string: String,
    loc: TokenLocation
): ASTValue("string", loc) {
    override fun toString(): String =
        "$name: $string"
}

class ASTNumber(
    val number: Double,
    loc: TokenLocation
): ASTValue("number", loc) {
    override fun toString(): String =
        "$name: $number"
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