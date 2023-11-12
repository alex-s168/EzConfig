package me.alex_s168.ezcfg.i

import java.io.File
import java.nio.file.Path

class FileSource(
    val path: Path
): Source {

    constructor(path: String):
            this(Path.of(path))

    constructor(file: File):
            this(file.toPath())

    override fun read(): String =
        path.toFile().readText()

    override fun path(): Path =
        path

}