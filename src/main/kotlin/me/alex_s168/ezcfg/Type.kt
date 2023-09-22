package me.alex_s168.ezcfg

enum class TypeBase {
    STRING,
    NUMBER,
    ARRAY,      // of
    FUNCTION,   // of
    BLOCK,
}

data class Type(
    val type: TypeBase,
    val of: Type? = null,
)