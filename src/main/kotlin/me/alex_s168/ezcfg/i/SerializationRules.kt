package me.alex_s168.ezcfg.i

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
annotation class SerializationRules<T>(
    val function: KClass<out SerializationFunction<T>>,
)