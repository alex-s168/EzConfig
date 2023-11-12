package me.alex_s168.ezcfg.i

import java.nio.file.Path

interface Source {

    fun read(): String

    fun path(): Path

}