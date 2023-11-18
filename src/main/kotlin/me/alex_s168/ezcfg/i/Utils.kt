package me.alex_s168.ezcfg.i

import me.alex_s168.ezcfg.ErrorContext
import me.alex_s168.ezcfg.i.internal.apply
import me.alex_s168.ezcfg.ast.ASTBlock
import me.alex_s168.ktlib.tree.Node

@Suppress("UNCHECKED_CAST")
fun <T> Element.apply(obj: T, ctx: ErrorContext): T =
    (this as Node<ASTBlock>).apply(obj, ctx)