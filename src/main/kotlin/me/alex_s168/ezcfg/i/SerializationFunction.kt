package me.alex_s168.ezcfg.i

import me.alex_s168.ezcfg.ErrorContext

interface SerializationFunction<T> {
    fun deserialize(element: Element, errorContext: ErrorContext): T
}