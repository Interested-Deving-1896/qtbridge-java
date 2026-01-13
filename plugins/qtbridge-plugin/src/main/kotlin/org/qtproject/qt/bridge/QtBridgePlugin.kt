/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.Action
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import javax.inject.Inject
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.qtproject.qt.bridge.extension.QmlExtension
import org.qtproject.qt.bridge.extension.QtBridgeAppExtension
import org.qtproject.qt.bridge.extension.QtBridgeExtension
import org.qtproject.qt.bridge.resolver.QtPathResolverFactory
import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qt.bridge.utils.EnvironmentConfigurator
import org.qtproject.qt.bridge.utils.QtPluginMode
import org.qtproject.qt.bridge.utils.exception.QtResourceExceptionHandler
import org.qtproject.qt.bridge.utils.prop
import org.qtproject.qtbridge.PluginVersion
import java.io.OutputStream
import java.net.URI

// Inject ExecOperations service for invoking external processes (eg. qmltyperegistrar)
abstract class QtBridgePlugin @Inject constructor(private val execOps: ExecOperations) : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create(
            "qtBridge",
            QtBridgeExtension::class.java,
            target
        )

        // Create nested extensions
        val appExtension = (extension as ExtensionAware).extensions.create(
            "application",
            QtBridgeAppExtension::class.java,
            target
        )

        val qmlExtension = extension.extensions.create(
            "qml",
            QmlExtension::class.java,
            target
        )

        target.repositories.apply {
            mavenCentral()
            // TODO:QTBUG-139181 It will be removed later when QtBridge is in stable release in MavenCentral
            maven { url = URI("https://android.qt.io/maven/releases") }
        }
        configureDependencies(target)

        target.afterEvaluate {
            configureQtBridge(this, extension, qmlExtension, appExtension)
            registerQmldirAndQmltypesTasks(this, extension)
            registerQmllsBuildIniTask(this, extension)
            registerQmllsTask(this, extension)
            registerQmllintTask(this, extension)
            createRunTask(this, appExtension)
        }
    }

    fun configureQtBridge(
        project: Project,
        extension: QtBridgeExtension,
        qmlExtension: QmlExtension,
        appExtension: QtBridgeAppExtension
    ) {
        val qtPluginMode = getCurrentPluginMode(project, appExtension.name.orNull)
        val includedBridgeBuild = project.gradle.includedBuilds.find { it.name == "qmlbridge" }
        if (qtPluginMode == QtPluginMode.DEV && includedBridgeBuild != null) {
            val buildNativeTask = includedBridgeBuild.task(":generateLibsAndCopy")
            project.tasks.withType<JavaExec>().configureEach {
                dependsOn(buildNativeTask)
            }
        } else {
            //Configure the environment
            val errorHandler = QtResourceExceptionHandler(project.logger)
            try {
                val defaultResolver = QtPathResolverFactory.default(project)
                val qtLibsDirPath = defaultResolver.libsResolver.resolve(extension.qtLibraryPath.orNull)
                val qtBridgeNativeLibDirPath = defaultResolver.bridgeNativeResolver.resolve(extension.qtBridgeLibraryPath.orNull)
                configureTaskEnvironments(project, qtLibsDirPath, qtBridgeNativeLibDirPath)
            } catch (e: Exception) {
                errorHandler.handle(e)
            }
        }
        // Configure Runtime environment
        configureRuntimeEnvironment(project, qmlExtension, appExtension)
    }

    private fun getCurrentPluginMode(project: Project, name: String?): QtPluginMode {
        val requestArgs = project.gradle.startParameter.taskRequests.flatMap { it.args }
        val isTargetingThisProject = if (requestArgs.isEmpty()) {
            true
        } else {
            requestArgs.any { arg ->
                when {
                    //explicit matching
                    arg.equals(name, ignoreCase = true) -> true
                    //loose matching
                    project.path.contains(arg, ignoreCase = false) -> true
                    else -> false
                }
            }
        }
        var qtPluginMode = QtPluginMode.fromString(project.prop("qt.plugin.mode"))

        if (!isTargetingThisProject) {
            qtPluginMode = QtPluginMode.DEV
        }
        return qtPluginMode
    }

    fun configureRuntimeEnvironment(
        project: Project,
        qmlExtension: QmlExtension,
        appExtension: QtBridgeAppExtension
    ) {
        if (Platform.isMacOS()) {
            project.tasks.withType<JavaExec>().configureEach {
                jvmArgs("-XstartOnFirstThread")
            }
        }

        //Configure QML Files
        configureQmlFiles(project, qmlExtension)

        //Configure Application
        configureApplication(project, appExtension)
    }

    private fun configureApplication(project: Project, appExtension: QtBridgeAppExtension) {
        project.tasks.withType(JavaExec::class.java).configureEach {
            // Set main class if provided
            appExtension.mainClass.orNull?.let { mainClassName ->
                mainClass.set(mainClassName)
                project.logger.info("Main class set to: $mainClassName")
            }

            // Add custom JVM arguments if provided
            val customJvmArgs = appExtension.jvmArgs.getOrElse(emptyList())
            if (customJvmArgs.isNotEmpty()) {
                jvmArgs(customJvmArgs)
                project.logger.info("JVM args added: ${customJvmArgs.joinToString(", ")}")
            }
        }
    }

    private fun configureQmlFiles(project: Project, extension: QmlExtension) {
        val mainQmlFile = extension.resolveMainQmlFile()
        val separator = if (Platform.isWindows()) ";" else ":"
        val allImportPaths = extension.resolveImportPaths().joinToString(separator)

        project.tasks.withType(JavaExec::class.java).configureEach {
            if (mainQmlFile != null && mainQmlFile.exists()) {
                project.logger.info("Main QML file: ${mainQmlFile.absolutePath}")
                systemProperty("qt.main.qml", mainQmlFile.absolutePath)
            }
            val sourceDirPath = extension.resolveSourceDirectory()?.absolutePath ?: ""
            environment("QML_IMPORT_PATH", allImportPaths + sourceDirPath)

            if (extension.importTrace.getOrElse(false)) {
                environment("QML_IMPORT_TRACE", "1")
            }
        }
    }

    private fun configureTaskEnvironments(project: Project, qtLibsDirPath: String, qtBridgeLibraryPath: String) {
        val environmentConfig = EnvironmentConfigurator(project.logger, qtLibsDirPath, qtBridgeLibraryPath)
        project.tasks.withType(JavaExec::class.java).configureEach {
            environmentConfig.configure(this)
        }
    }

    private fun configureDependencies(project: Project) {
        val dependency = resolveQmlBridgeDependency()
        if (!project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
            project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        }
        if(!project.pluginManager.hasPlugin("com.google.devtools.ksp")){
            project.pluginManager.apply("com.google.devtools.ksp")
        }
        project.dependencies.apply {
            add("implementation", dependency)
            add("ksp", dependency)
        }
    }

    private fun resolveQmlBridgeDependency(): String {
        val pluginVersion = PluginVersion.VERSION
        val resolvedVersion = if (pluginVersion.contains("+") || pluginVersion.count { it == '.' } < 2) {
            "$pluginVersion.+"
        } else {
            pluginVersion
        }
        return "org.qtproject.qt.bridge:qmlbridge:$resolvedVersion"
    }

    private fun createRunTask(project: Project, appExtension: QtBridgeAppExtension) {
        if (appExtension.mainClass.isPresent) {
            val taskName = appExtension.name.getOrElse(project.name)

            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            project.tasks.register(taskName, JavaExec::class.java) {
                group = "application"
                description = "Run the Qt Bridge ${project.name} name"
                classpath = mainSourceSet.runtimeClasspath
                mainClass.set(appExtension.mainClass)
            }
            project.logger.info("Created '$taskName' task for main class: ${appExtension.mainClass.get()}")
        }
    }

    // Directory provider for application QML imports (qmldir and qmltypes files)
    private fun appQmlImportDirProvider(project: Project,
            sourceSetName: String = MAIN_SOURCE_SET_NAME): Provider<Directory>
        = project.layout.buildDirectory.dir("qtbridge/qml-imports/$sourceSetName")

    // Directory provider for generated MOC JSON files
    private fun mocJsonDirProvider(project: Project,
            sourceSetName: String = MAIN_SOURCE_SET_NAME): Provider<Directory>
        = project.layout.buildDirectory.dir("generated/ksp/$sourceSetName/resources")

    // File provider for generated .qmlls.build.ini
    private fun qmllsIniFileProvider(project: Project): Provider<RegularFile>
        = project.layout.buildDirectory.file(".qt/.qmlls.build.ini")

    // Resolves Qt base directory (containing bin, lib, doc, qml, (libexec) ...).
    // This base directory can then be used to derive paths to tools and directories.
    // The precedence is:
    // - QTBRIDE_QTDIR environment variable override. Useful for development purposes
    // - QtPathResolverFactory
    // TODO: could the logic be moved to the resolver (maybe create new qtdirResolver)?
    private fun resolveQtDir(project: Project, extension: QtBridgeExtension): java.io.File? {
        // Helper for testing if the directory seems correct
        fun isProperQtDir(dir: java.io.File): Boolean {
            if (!dir.isDirectory) return false
            val bin = dir.resolve("bin")
            if (!bin.isDirectory) return false

            // Use the presence of qmllint executable to decide if it's a proper directory
            val qmllint = if (Platform.isWindows()) "qmllint.exe" else "qmllint"
            val qmllintExecutable = bin.resolve(qmllint)
            if (!qmllintExecutable.exists()) return false

            // Single out this error scenario as it can be obscure to pinpoint
            if (!qmllintExecutable.canExecute()) {
                project.logger.lifecycle("Resolved $qmllintExecutable tool but it's not executable");
                return false
            }
            return true
        }

        // Check if we have an explicit environment variable override we should use
        val qtdir = System.getenv("QTBRIDGE_QTDIR")
            ?.takeIf { it.isNotBlank() }
            ?.let { java.io.File(it).canonicalFile }

        if (qtdir != null && isProperQtDir(qtdir)) {
            project.logger.lifecycle("Resolved Qt dir from QTBRIDGE_QTDIR env variable: ${qtdir.absolutePath}")
            return qtdir
        }

        // Check if we can use the resolved Qt
        try {
            val resolver = QtPathResolverFactory.withoutDownloadProviders(project)
            val qtLibDirPath = resolver.libsResolver.resolve(extension.qtLibraryPath.orNull)
            val libDir = java.io.File(qtLibDirPath).canonicalFile

            // Assume qtDir to be the parent of 'lib'
            val qtDir = libDir.parentFile?.canonicalFile
            if (qtDir != null && isProperQtDir(qtDir)) {
                project.logger.lifecycle("Resolved Qt dir with QtPathResolver: ${qtDir.absolutePath}")
                return qtDir
            }
        } catch (_: Exception) {
            // ignore and fall through
        }
        return null
    }

    private fun resolveQmllsExecutable(project: Project, extension: QtBridgeExtension): String? {
        val qtDir = resolveQtDir(project, extension) ?: return null
        val executableName = if (Platform.isWindows()) "qmlls.exe" else "qmlls"
        val executable = qtDir.resolve("bin").resolve(executableName)

        return executable.takeIf { it.exists() && it.canExecute() }
            ?.also { project.logger.lifecycle("Using qmlls: ${it.absolutePath}") }
            ?.absolutePath
    }

    private fun resolveQmllintExecutable(project: Project, extension: QtBridgeExtension): String? {
        val qtDir = resolveQtDir(project, extension) ?: return null
        val executableName = if (Platform.isWindows()) "qmllint.exe" else "qmllint"
        val executable = qtDir.resolve("bin").resolve(executableName)

        return executable.takeIf { it.exists() && it.canExecute() }
            ?.also { project.logger.lifecycle("Using qmllint: ${it.absolutePath}") }
            ?.absolutePath
    }

    private fun resolveQmlTypeRegistrarExecutable(project: Project, extension: QtBridgeExtension): String? {
        val qtDir = resolveQtDir(project, extension) ?: return null
        val executableName = if (Platform.isWindows()) "qmltyperegistrar.exe" else "qmltyperegistrar"
        val executable = if (Platform.isWindows()) qtDir.resolve("bin").resolve(executableName) else qtDir.resolve("libexec").resolve(executableName)

        return executable.takeIf { it.exists() && it.canExecute() }?.also {
            project.logger.lifecycle("Using qmltyperegistrar: ${it.absolutePath}")
        }?.absolutePath
    }

    private fun resolveQtDocDir(project: Project, extension: QtBridgeExtension): String? {
        val qtDir = resolveQtDir(project, extension) ?: return null
        val qtDocDir = qtDir.resolve("doc")
        // Sanity check that the doc dir has a 'global' directory
        val globalDir = qtDocDir.resolve("global")

        return qtDocDir.takeIf { it.exists() && globalDir.isDirectory }
            ?.also { project.logger.lifecycle("Using Qt doc directory: ${it.absolutePath}") }
            ?.absolutePath
    }

    private fun resolveQmlImportDir(project: Project, extension: QtBridgeExtension): String? {
        val qtDir = resolveQtDir(project, extension) ?: return null
        val qtDocDir = qtDir.resolve("qml")
        // Sanity check that the qml dir has a 'QtQuick' directory
        val globalDir = qtDocDir.resolve("QtQuick")

        return qtDocDir.takeIf { it.exists() && globalDir.isDirectory }
            ?.also { project.logger.lifecycle("Using QML import directory for base Qt imports: ${it.absolutePath}") }
            ?.absolutePath
    }

    private fun registerQmldirAndQmltypesTasks(project: Project, extension: QtBridgeExtension) {
        // Main task for producing QML tooling files
        val qmldirAndQmltypes = project.tasks.register("qmldirAndQmltypes") {
            group = "build"
            description = "Generates qmldir + .qmltypes for Qt Bridge modules."
        }

        // Helper for deriving QML module URI from the file name. For example
        // my.awesome.module_moc.json => my.awesome.module
        fun moduleUriFromMocJson(fileName: String): String? =
            fileName.takeIf { it.endsWith("_moc.json") }?.removeSuffix("_moc.json")

        // Helper for derive QML module directory from the URI. For example
        // my.awesome.module => my/awesome/module
        fun moduleDirForUri(root: java.io.File, uri: String): java.io.File =
            java.io.File(root, uri.replace('.', java.io.File.separatorChar))

        // Only generate QML tooling files for 'main' source set. Generating for 'test' could
        // create a dependency loop: classes -> qmldirAndQmltypes-> ... -> kspTestKotlin -> classes
        // See QTBUG-143098 for processing more source sets.
        val sourceSetName = MAIN_SOURCE_SET_NAME

        // Look for the KSP task. KSP task for 'main' source set is always 'kspKotlin'
        val kspTaskProvider =
            project.tasks.matching { it.name == "kspKotlin" }
                .takeIf { !it.isEmpty() }
                ?.let { project.tasks.named("kspKotlin") }

        // 'generate' task: run qmltyperegistrar + write qmldir files
        val generate = project.tasks.register("qtbridgeGenerateQmldirAndQmltypes") {
            group = "build"
            description = "Generates qmldir + .qmltypes for sourceSet $sourceSetName."

            // Ensure that 'generate' runs whenever moc files change, and that KSP has run
            if (kspTaskProvider != null) dependsOn(kspTaskProvider)
            inputs.dir(mocJsonDirProvider(project))
                .withPropertyName("mocJsonDir")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            // Where we generate QML qmldir + plugins.qmltypes outputs
            outputs.dir(appQmlImportDirProvider(project))

            doLast {
                val registrarExe = resolveQmlTypeRegistrarExecutable(project, extension)
                if (registrarExe == null) {
                        project.logger.lifecycle(
                            "qmltyperegistrar not found. " +
                            "Set -Pqtbridge.qmltyperegistrar=/abs/path/to/qmltyperegistrar, " +
                            "configure qtBridge.qtLibraryPath so we can derive the Qt bin dir, or " +
                            "have qmltyperegistrar executable on PATH. Lack of qmltyperegistrar " +
                            "impacts QML tooling support, but not the application run itself."
                        )
                    return@doLast // skip qmltyperegistar run since not found
                }

                // Get all <module>_moc.json files
                val mocFiles = project.fileTree(mocJsonDirProvider(project).get().asFile) { include("**/*_moc.json") }.files
                if (mocFiles.isEmpty()) {
                        project.logger.lifecycle("No *_moc.json files found under " +
                                                 "${mocJsonDirProvider(project).get().asFile.absolutePath} " +
                                                 "for sourceSet $sourceSetName.")
                    return@doLast
                }

                // Handle each <module>_moc.json file. They are already per-QML-module
                mocFiles.forEach { mocFile ->
                    val uri = moduleUriFromMocJson(mocFile.name) ?: return@forEach

                    val moduleDir = moduleDirForUri(appQmlImportDirProvider(project).get().asFile, uri)
                    moduleDir.mkdirs()

                    val qmltypesFile = java.io.File(moduleDir, "plugins.qmltypes")
                    val qmldirFile = java.io.File(moduleDir, "qmldir")

                    // Generate .qmltypes (quiet by default, QTBUG-143092)
                    execOps.exec(object : Action<ExecSpec> {
                        override fun execute(spec: ExecSpec) {
                            spec.executable = registrarExe
                            spec.args(
                                "--generate-qmltypes",
                                qmltypesFile.absolutePath,
                                "--import-name",
                                uri,
                                "--major-version",
                                "1",
                                "--minor-version",
                                "0",
                                mocFile.absolutePath
                            )
                            if (true) {
                                // QTBUG-143092: allow output when possible. Currently qmltyperegistrar
                                // attempts to generate C++ registration code, which results in many warnings
                                spec.standardOutput = OutputStream.nullOutputStream()
                                spec.errorOutput = OutputStream.nullOutputStream()
                            }
                        }
                    })

                    // Generate qmldir next to .qmltypes. This qmldir is very minimalistic
                    // with just: module + typeinfo + minimal dependency (QTBUG-143190).
                    // Dependencies likely need changes when we implement QTBUG-143107,
                    // the support for adding QML files to the modules.
                    qmldirFile.writeText(
                        buildString {
                            appendLine("module $uri")
                            appendLine("typeinfo plugins.qmltypes")
                            // Hardcoded dependency for QAbstractItemModel, see QTBUG-143190
                            appendLine("import QtQml.Models auto")
                        }
                    )
                }
            }
        }

        // Main task depends on qmldir and .qmltypes generation
        qmldirAndQmltypes.configure { dependsOn(generate) }

        // Ensure `./gradlew <app>` will run QML tooling
        project.tasks.matching { it.name == "classes" }.configureEach {
            dependsOn(qmldirAndQmltypes)
        }
        // Ensure `./gradlew check` will generate qmldir and qmltypes files
        project.tasks.matching { it.name == "check" }.configureEach {
            dependsOn(qmldirAndQmltypes)
        }
    }

    private fun qmllsSectionNameFromPath(path: java.io.File): String {
        // .qmlls.build.ini uses <SLASH> escaping for '/'
        val p = path.absolutePath.replace('\\', '/')
        return p.split('/').joinToString(separator = "") { part ->
            if (part.isEmpty()) "" else "<SLASH>$part"
        }
    }

    // Sets up a task for writing .qmlls.build.ini file. qmlls from 6.10.0 reads
    // the .qmlls.build.ini file from the build folder to figure out import paths,
    // documentation paths, etc.
    private fun registerQmllsBuildIniTask(project: Project, extension: QtBridgeExtension) {
        // QML source files
        val qmlSourceDir = project.projectDir

        val generateIni = project.tasks.register("qtbridgeGenerateQmllsBuildIni") {
            group = "build"
            description = "Generates build/.qt/.qmlls.build.ini for qmlls (Qt 6.10+)."

            // Make sure the import tree / dependencies exists before writing ini
            dependsOn("qmldirAndQmltypes")

            // Depend on QML module contents
            inputs.dir(appQmlImportDirProvider(project))
                .withPropertyName("appQmlImportRoot")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            // Where the .qmlls.build.ini goes (as expected by qmlls)
            outputs.file(qmllsIniFileProvider(project))

            doLast {
                val iniFile = qmllsIniFileProvider(project).get().asFile
                iniFile.parentFile.mkdirs()

                val sep = if (Platform.isWindows()) ";" else ":"

                // Import paths for qmlls
                val importPaths = mutableListOf<String>()
                // 1) Generated application QML modules
                importPaths += appQmlImportDirProvider(project).get().asFile.absolutePath
                // 2) Imports for main Qt (QtQuick, QtQuick.Controls, ...)
                resolveQmlImportDir(project, extension)?.let { importPaths += it }

                val docDir = resolveQtDocDir(project, extension)
                val sectionName = qmllsSectionNameFromPath(qmlSourceDir)

                val text = buildString {
                    appendLine("[General]")
                    if (docDir != null) appendLine("docDir=$docDir")
                    appendLine()
                    appendLine("[$sectionName]")
                    appendLine("importPaths=\"${importPaths.distinct().joinToString(sep)}\"")
                    appendLine()
                }
                iniFile.writeText(text)

                project.logger.lifecycle(
                    "Wrote ${iniFile.absolutePath}. Run qmlls with: qmlls -b ${project.layout.buildDirectory.get().asFile.absolutePath}"                )
            }
        }
        // Ensure `./gradlew check` creates ini files
        project.tasks.matching { it.name == "check" }.configureEach { dependsOn(generateIni) }
    }

    // Task for printing out the qmlls usage per application. Task is intended for
    // development and debugging purposes, as it prints the necessary command to run
    // (but other than that, running qmlls on command line is not all that useful)
    private fun registerQmllsTask(project: Project, extension: QtBridgeExtension) {
        project.tasks.register("qtbridgeQmlls") {
            group = "verification"
            description = "Shows how to run qmlls against this build (requires Qt 6.10+)."
            dependsOn("qtbridgeGenerateQmllsBuildIni")

            doLast {
                val qmlls = resolveQmllsExecutable(project, extension)
                val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
                if (qmlls == null) {
                    project.logger.lifecycle(
                        "qmlls not found. Set -Pqtbridge.qmlls=/abs/path/to/qmlls, or configure qtBridge.qtLibraryPath, or put qmlls on PATH."
                    )
                    return@doLast
                }
                project.logger.lifecycle("qmlls found: $qmlls")
                project.logger.lifecycle("Run: $qmlls -b $buildDir")
            }
        }
    }

    // Task for testing the qmllinting, runs the qmllint for a given project. For instance:
    // ./gradlew :examples:manualtest:qtbridgeQmllint
    private fun registerQmllintTask(project: Project, extension: QtBridgeExtension) {
        val qmlSourceDir = project.projectDir
        project.tasks.register("qtbridgeQmllint") {
            group = "verification"
            description = "Runs qmllint on QML sources using Qt Bridge generated import tree."

            dependsOn("qmldirAndQmltypes")

            inputs.dir(appQmlImportDirProvider(project))
                .withPropertyName("appQmlImportRoot")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            doLast {
                val qmllint = resolveQmllintExecutable(project, extension)
                if (qmllint == null) {
                    project.logger.lifecycle(
                        "qmllint not found. Set -Pqtbridge.qmllint=/abs/path/to/qmllint, or configure qtBridge.qtLibraryPath, or put qmllint on PATH."
                    )
                    return@doLast
                }

                val importPaths = mutableListOf<String>()
                // Add application project QML module(s) to QML import path
                importPaths += appQmlImportDirProvider(project).get().asFile.absolutePath
                // Add base Qt imports to import path
                resolveQmlImportDir(project, extension)?.let { importPaths += it }

                // Find the *.qml source files to lint
                val qmlFiles = project.fileTree(qmlSourceDir) { include("**/*.qml") }.files
                if (qmlFiles.isEmpty()) {
                    project.logger.lifecycle("No .qml files found under ${qmlSourceDir.absolutePath}")
                    return@doLast
                }

                qmlFiles.forEach { qmlFile ->
                    val args = ArrayList<String>()

                    // -I for each QML import path
                    importPaths.distinct().forEach { importPath ->
                        args += listOf("-I", importPath)
                    }

                    // Avoid default import directories and current working directory
                    args += "--bare"
                    args += qmlFile.absolutePath

                    // Print the command for debugging purposes
                    val quoteAndEscape = { value: String ->
                        val needsProcessing = value.any { it.isWhitespace() || it == '"' }
                        // Surround with quotes and escape any embedded "
                        if (needsProcessing) "\"${value.replace("\"", "\\\"")}\"" else value
                    }
                    val printableCommand = (listOf(qmllint) + args).joinToString(" ") { quoteAndEscape(it) }
                    project.logger.lifecycle("qmllint command: $printableCommand")

                    execOps.exec(object : Action<ExecSpec> {
                        override fun execute(spec: ExecSpec) {
                            spec.executable = qmllint
                            spec.args(args)
                        }
                    })
                }
            }
        }
        // qmllint could be part of 'check' task, but currently example apps have too many warnings
        // project.tasks.matching { it.name == "check" }.configureEach { dependsOn(task) }
    }
}
