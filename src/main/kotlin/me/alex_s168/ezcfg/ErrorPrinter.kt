package me.alex_s168.ezcfg

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ezcfg.tokens.TokenLocation
import me.alex_s168.ktlib.async.concurrentMutableCollectionOf
import kotlin.math.max
import kotlin.math.min

data class Error(
    val loc: TokenLocation,
    val msg: String?,
    val isWarn: Boolean
)

data class ErrorContext(
    val name: String,
    val errors: MutableCollection<Error> = concurrentMutableCollectionOf()
)

fun ErrorContext.addError(loc: TokenLocation, msg: String?) {
    errors += Error(loc, msg, false)
}

fun ErrorContext.addWarn(loc: TokenLocation, msg: String?) {
    errors += Error(loc, msg, true)
}

fun ErrorContext.done() {
    if (errors.isEmpty()) return

    val warnCount = errors.count { it.isWarn }
    val errCount = errors.count { !it.isWarn }

    if (warnCount > 0) {
        if (errCount > 0) {
            println(TextColors.yellow("$errCount errors and $warnCount warnings during $name:"))
        } else {
            println(TextColors.yellow("$warnCount warnings during $name:"))
        }
    }
    else {
        println(TextColors.red("$errCount errors during $name:"))
    }

    val files = errors.groupBy {
        it.loc.rootLocation.file
    }

    if (files.isNotEmpty()) {
        println(TextColors.gray("========================================"))
    }
    files.forEach { (file, errs) ->
        println(TextColors.gray("In file $file"))
        println()
        errs.sortedBy {
            it.loc.line * 1000 + it.loc.column
        }.forEach {
            val color = if (it.isWarn) TextColors.yellow else TextColors.red
            printLoc(it.loc, color, it.msg)
            println()
        }
        println(TextColors.gray("========================================"))
    }

    if (errCount > 0) {
        throw ConfigException("Cannot continue due to $errCount errors during $name")
    }
}

private fun printLoc(loc: TokenLocation, color: TextColors, msg: String?) {
    val leftUnc = "${loc.line} | "
    val left = TextColors.gray(leftUnc)
    print(left)
    val line = loc.code.split("\n")[loc.line - 1]

    val pre = line.substring(0, loc.column - loc.length)
    val mid = TextStyles.bold(line.substring(loc.column - loc.length, min(line.length, loc.column + loc.length- loc.length)))
    val post = line.substring(min(line.length, loc.column + loc.length - loc.length))

    print(pre)
    print(mid)
    print(post)
    println()

    val ll = leftUnc.length + pre.length
    print(" ".repeat(ll))
    print(color("^" + "~".repeat(loc.length - 1)))
    println()

    msg?.let {
        val mll = max(0, ll + loc.length / 2 - msg.length / 2)
        print(" ".repeat(mll))
        print(color(msg))
        print(TextColors.gray(" (${loc.line}:${loc.column})"))
        println()
    }
}