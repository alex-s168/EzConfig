package me.alex_s168.ezcfg.i

import me.alex_s168.ezcfg.ErrorContext

interface SpecialSerializable<S> {
    fun deserialize(element: Element, errorContext: ErrorContext)
}