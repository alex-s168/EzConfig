package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.ast.*
import me.alex_s168.ezcfg.check.TypeBase
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ktlib.tree.Node
import java.lang.Exception
import java.lang.reflect.Constructor

@Suppress("UNCHECKED_CAST")
internal fun <T> conv(clazz: Class<T>, value: Node<ASTValue>): T? {
    if (clazz.isArray) {
        if (value.value!!.type!!.type != TypeBase.ARRAY) {
            throw ConfigException("Expected array, got ${value.value!!.type!!.type}")
        }
        val iarr = (value.value!! as ASTArray).content
        val arr = java.lang.reflect.Array.newInstance(clazz.componentType, iarr.size)
        iarr.forEachIndexed { i, it ->
            val x = conv(clazz.componentType, it)
            java.lang.reflect.Array.set(arr, i, x)
        }
        return arr as T
    }

    when (clazz) {
        // java non-primitive types
        Array<Int>::class.java.componentType -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toInt() as T
        }
        Array<Long>::class.java.componentType -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toLong() as T
        }
        Array<Float>::class.java.componentType -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toFloat() as T
        }
        Array<Double>::class.java.componentType -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number as T
        }
        Array<Boolean>::class.java.componentType -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return ((value.value!! as ASTNumber).number.toInt() != 0) as T
        }

        String::class.java -> {
            if (value.value!!.type!!.type != TypeBase.STRING) {
                throw ConfigException("Expected string, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTString).string as T
        }

        // java primitive types
        Int::class.java -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toInt() as T
        }
        Long::class.java -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toLong() as T
        }
        Float::class.java -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toFloat() as T
        }
        Double::class.java -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number as T
        }
        Boolean::class.java -> {
            if (value.value!!.type!!.type != TypeBase.NUMBER) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return ((value.value!! as ASTNumber).number.toInt() != 0) as T
        }
    }

    return null
}

@Suppress("UNCHECKED_CAST")
fun <T> Node<ASTBlock>.apply(obj: T): T {
    val clazz = obj!!::class.java
    this.value!!.variables.forEach { variable ->
        val valueO = variable.value
        try {
            val f = clazz.getDeclaredField(variable.name)
            f.isAccessible = true

            val v = conv(f.type, valueO)
            if (v != null) {
                f.set(obj, v)
            }
            else {
                if (valueO.value !is ASTBlock) {
                    throw ConfigException("Expected block, got ${valueO.value!!.type!!.type}")
                }

                val c: Constructor<*>
                try {
                    c = f.type.getConstructor()
                } catch (_: Exception) {
                    throw ConfigException("Expected default constructor for ${f.type}!")
                }
                c.isAccessible = true
                val o = c.newInstance()
                (valueO as Node<ASTBlock>).apply(o)
                f.set(obj, o)
            }
        } catch (_: Exception) {}
    }
    return obj
}