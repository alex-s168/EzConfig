package me.alex_s168.ezcfg

import me.alex_s168.ezcfg.exception.ConfigException
import me.alex_s168.ktlib.async.AsyncTask
import me.alex_s168.ktlib.async.async
import me.alex_s168.ktlib.async.concurrentMutableCollectionOf
import me.alex_s168.ktlib.async.forEachAsyncConf
import me.alex_s168.ktlib.tree.MutableNode
import me.alex_s168.ktlib.tree.MutableTree
import me.alex_s168.ktlib.tree.Node
import me.alex_s168.ktlib.tree.traverser.AsyncTreeTraverser
import java.nio.file.Path
import kotlin.io.path.pathString

@Suppress("UNCHECKED_CAST")
internal fun generateASTFor(
    input: String,
    path: Path,
    tree: MutableTree<ASTValue>,
    tokenizingErrors: ErrorContext,
    parserErrors: ErrorContext,
    processingErrors: ErrorContext
): Iterable<AsyncTask> {
    val rootLoc = RootTokenLocation(path.toAbsolutePath().toString())

    val tokens = tokenize(input, rootLoc, tokenizingErrors)

    val parsedNode =
        parseMain(tokens, parserErrors)

    val astFile = ASTFile(
        path = path.toAbsolutePath(),
        loc = TokenLocation(
            line = 0,
            column = 0,
            length = 0,
            code = input,
            rootLocation = rootLoc
        )
    )

    val fileNode = MutableNode(
        value = astFile,
        children = parsedNode.children,
        parent = tree.root
    )

    parsedNode.await()

    val allVariables = concurrentMutableCollectionOf<Iterable<Variable>>(astFile.variables)

    fileNode.children.forEach {
        if (it.value is ASTAssignment) {
            val name = (it.children.first().value as? ASTVariableReference
                ?: throw ConfigException("Unexpected error occurred! [gAST:rCtF:3]"))
                .variable

            if (astFile.variables.any { v -> v.name == name }) {
                processingErrors.addWarn(
                    it.value!!.loc,
                    "Variable already defined!"
                )
            }

            val variable = Variable(
                name,
                it.value!!.type,
                it.children.last(),
            )

            astFile.variables += variable
        }
    }
    
    AsyncTreeTraverser.from(fileNode) { node, _ ->
        node as? MutableNode<ASTValue>
            ?: throw ConfigException("Unexpected error occurred! [gAST:rCtF:1]")

        if (node.value is ASTBlock) {
            val b = (node.value!! as ASTBlock)
            allVariables += b.variables
            node.children.forEach { child ->
                val ass = child.value as? ASTAssignment
                    ?: return@forEach

                val name = (child.children.first().value as? ASTVariableReference
                    ?: throw ConfigException("Unexpected error occurred! [gAST:rCtF:3:x]"))
                    .variable

                if (b.variables.any { it.name == name }) {
                    processingErrors.addWarn(
                        child.value!!.loc,
                        "Variable already defined!"
                    )
                }

                val variable = Variable(
                    name,
                    ass.type,
                    child.children.last(),
                )

                b.variables += variable
            }
        }

        true
    }.traverse()

    // update parents because calculateTypes needs the parents
    AsyncTreeTraverser.from(fileNode) { node, _ ->
        node as MutableNode
        node.children.forEach {
            it.parent = node
        }
        return@from true
    }.traverse()

    fileNode.calculateTypes(processingErrors, fileNode)

    // now we can update all the types of all the variables
    allVariables.forEach {
        it.forEach {  v ->
            v.type = v.value.value?.type
        }
    }

    tree += fileNode

    val root = tree.root.value!! as ASTRoot

    root.imports.add(path.toAbsolutePath())

    val importTasks = concurrentMutableCollectionOf<AsyncTask>()

    fileNode.children.forEachAsyncConf { statement ->
        if (statement.value is ASTFunctionCall) {
            val func = statement.children.first()
            val name = (func.value!! as? ASTVariableReference)?.variable
                ?: throw ConfigException("Unexpected error occurred! [gAST:rCtF:1]")

            val arg = statement.children.last()

            val ft = when (name) {
                "include" -> { 0 }
                "export" -> { 1 }
                "native" -> { 2 }
                else -> { return@forEachAsyncConf }
            }

            if (arg == func) {
                processingErrors.addError(
                    func.value!!.loc,
                    "Expected string literal as argument!"
                )
                return@forEachAsyncConf
            }

            val argv = arg.value
                ?: throw ConfigException("Unexpected error occurred! [gAST:rCtF:2]")

            if (argv !is ASTString) {
                processingErrors.addError(
                    argv.loc,
                    "Expected string literal!"
                )
            }

            argv as ASTString

            if (ft == 0) { // include
                val p = getPath(argv.string, path.parent, root)
                if (p == null) {
                    processingErrors.addError(
                        argv.loc,
                        "File not found!"
                    )
                    return@forEachAsyncConf
                }

                val f = p.toFile()

                if (!f.exists()) {
                    processingErrors.addError(
                        argv.loc,
                        "File not found!"
                    )
                    return@forEachAsyncConf
                }

                if (p in root.imports) {
                    // already imported
                    return@forEachAsyncConf
                }

                // namespace
                val pns = p
                    .fileName
                    .pathString
                    .substringBeforeLast(".ezcfg")

                astFile.importedNamespaces += pns to p

                root.imports.add(p)

                importTasks += async {
                    importTasks += generateASTFor(
                        f.readText(),
                        p,
                        tree,
                        tokenizingErrors,
                        parserErrors,
                        processingErrors
                    )
                }

                return@forEachAsyncConf
            }

            val vn = argv.string

            val v = arg.resolve(vn, fileNode as Node<ASTFile>, processingErrors)
            if (v == null) {
                processingErrors.addError(
                    argv.loc,
                    "Variable not found!"
                )
                return@forEachAsyncConf
            }

            if (ft == 1) {
                v.exported = true
            } else {
                v.native = true
            }
        }
    }

    return importTasks
}