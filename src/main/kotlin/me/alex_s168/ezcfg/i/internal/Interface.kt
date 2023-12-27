package me.alex_s168.ezcfg.i.internal

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.addError
import me.alex_s168.ezcfg.ast.*
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ezcfg.i.SerializationRules
import me.alex_s168.ezcfg.i.SpecialSerializable
import me.alex_s168.ktlib.tree.Node
import java.lang.reflect.Constructor
import kotlin.Exception
import kotlin.reflect.full.createInstance

@Suppress("UNCHECKED_CAST")
internal fun <T> conv(clazz: Class<T>, value: Node<ASTValue>, errCtx: ErrorContext): T? {
    val deserializeMethod = try {
        clazz.getMethod("deserialize", Node::class.java, ErrorContext::class.java)
    } catch (e: Exception) {
        null
    }
    if (deserializeMethod != null) {
        val s: T
        try {
            s = clazz.getConstructor().newInstance()
        } catch (e: Exception) {
            throw ConfigException("Expected default constructor for ${clazz.canonicalName}!")
        }
        try {
            deserializeMethod.invoke(s, value, errCtx)
        } catch (e: Exception) {
            println(e)
        }
        return s
    }

    if (clazz.isEnum) {
        val enum = clazz as Class<out Enum<*>>
        enum.enumConstants
        if (value.value !is ASTEnumValue) {
            throw ConfigException("Expected enum, got ${value.value!!.type!!.type}")
        }
        val v = value.value!! as ASTEnumValue
        try {
            return java.lang.Enum.valueOf(enum, v.value) as T
        } catch (e: Exception) {
            val en = v.enum

            errCtx.addError(
                en.loc,
                "Enum contains value(s) that are not in ${enum.canonicalName}! (\"${v.value}\")"
            )
            
            return null
        }
    }

    if (clazz.isArray) {
        if (value.value !is ASTArray) {
            throw ConfigException("Expected array, got ${value.value!!.type!!.type}")
        }
        val iarr = (value.value!! as ASTArray).content
        val arr = java.lang.reflect.Array.newInstance(clazz.componentType, iarr.size)
        iarr.forEachIndexed { i, it ->
            val x = conv(clazz.componentType, it, errCtx)
            java.lang.reflect.Array.set(arr, i, x)
        }
        return arr as T
    }

    when (clazz) {
        // java non-primitive types
        Array<Int>::class.java.componentType -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toInt() as T
        }
        Array<Long>::class.java.componentType -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toLong() as T
        }
        Array<Float>::class.java.componentType -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toFloat() as T
        }
        Array<Double>::class.java.componentType -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number as T
        }
        Array<Boolean>::class.java.componentType -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return ((value.value!! as ASTNumber).number.toInt() != 0) as T
        }

        String::class.java -> {
            if (value.value!! !is ASTString) {
                throw ConfigException("Expected string, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTString).string as T
        }

        // java primitive types
        Int::class.java -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toInt() as T
        }
        Long::class.java -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toLong() as T
        }
        Float::class.java -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number.toFloat() as T
        }
        Double::class.java -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return (value.value!! as ASTNumber).number as T
        }
        Boolean::class.java -> {
            if (value.value!! !is ASTNumber) {
                throw ConfigException("Expected number, got ${value.value!!.type!!.type}")
            }
            return ((value.value!! as ASTNumber).number.toInt() != 0) as T
        }
    }

    return null
}

@Suppress("UNCHECKED_CAST")
fun <T> Node<ASTValue>.apply(obj: T, errCtx: ErrorContext): T {
    val clazz = obj!!::class.java
    if (clazz.isInstance(SpecialSerializable::class)) {
        val s = obj as SpecialSerializable<T>
        s.deserialize(this, errCtx)
        return obj
    }
    if (this.value !is ASTBlock) {
        return conv(clazz, this, errCtx)!!
    }
    this as Node<ASTBlock>
    this.value!!.variables.forEach { variable ->
        val valueO = variable.value
        try {
            val f = clazz.getDeclaredField(variable.name)
            f.isAccessible = true
            val serializationRules = try { f.getAnnotation(SerializationRules::class.java) } catch (_: Exception) { null }
            if (serializationRules != null) {
                f.set(obj, try {
                    serializationRules.function.createInstance()
                } catch (_: Exception) {
                    throw ConfigException("Expected default constructor for SerializationFunction instantiating classes!")
                }.deserialize(valueO, errCtx))
                return@forEach
            }

            val v = conv(f.type, valueO, errCtx)
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
                valueO.apply(o, errCtx)
                f.set(obj, o)
            }
        } catch (x: Exception) {}
    }
    return obj
}