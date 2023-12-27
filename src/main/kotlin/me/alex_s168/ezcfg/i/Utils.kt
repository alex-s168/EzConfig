package me.alex_s168.ezcfg.i

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.i.internal.apply

@Suppress("UNCHECKED_CAST")
fun <T> Element.apply(obj: T, ctx: ErrorContext): T =
    this.apply(obj, ctx)