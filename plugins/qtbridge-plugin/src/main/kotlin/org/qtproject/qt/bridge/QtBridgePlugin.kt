/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.qtproject.qt.bridge.extension.QmlExtension
import org.qtproject.qt.bridge.extension.QtBridgeAppExtension
import org.qtproject.qt.bridge.extension.QtBridgeExtension
import org.qtproject.qt.bridge.resolver.factory.QtResolverFactory
import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qt.bridge.utils.*
import org.qtproject.qt.bridge.utils.exception.QtResourceExceptionHandler
import java.io.File
import java.io.OutputStream
import java.net.URI
import javax.inject.Inject

// Inject ExecOperations service for invoking external processes (eg. qmltyperegistrar)
abstract class QtBridgePlugin @Inject constructor(private val execOps: ExecOperations) : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = createExtensions(target)

        target.repositories.apply {
            mavenCentral()
            //QTBUG-139181 : to remove when QtBridge is available in MavenCentral
            mavenLocal()
            maven { url = URI("https://android.qt.io/maven/releases") }
        }
        configureDependencies(target)
        target.afterEvaluate {
            val appExtension = (extension as ExtensionAware).extensions.getByType(QtBridgeAppExtension::class.java)
            val mode = getCurrentPluginMode(target, appExtension.name.orNull)

            val qtBridgeContext = QtBridgeContext(target, extension, mode)
            configureQtBridge(qtBridgeContext)
            registerTasks(qtBridgeContext)
            createRunTask(qtBridgeContext)
        }
    }

    private fun registerTasks(qtBridgeContext: QtBridgeContext) {
        val project = qtBridgeContext.project
        val qmlExtension = (qtBridgeContext.extension as ExtensionAware).extensions.getByType(QmlExtension::class.java)

        val resolverFactory = qtBridgeContext.qtResolverFactory
        val toolingInfo = createQmlToolingInfo(project, resolverFactory, qmlExtension)

        val qmlDirAndTypesTask = registerQmldirAndQmltypesTasks(
            project = project,
            qmlRegistrar = resolverFactory.qmlTypeRegistrarResolver.resolve()?.absolutePath
        )

        val qmllsBuildIniTask = registerQmllsBuildIniTask(
            project = project,
            generateQmldirAndQmltypesTask = qmlDirAndTypesTask,
            importPaths = toolingInfo.qmlImportPaths,
            docDir = toolingInfo.docDir
        )

        val qmlToolingInfoTask = registerQmlToolingInfoForIDETask(
            project = project,
            generateQmldirAndQmltypesTask = qmlDirAndTypesTask,
            qmllsBuildIniTask = qmllsBuildIniTask,
            toolingInfo = toolingInfo
        )

        // Try to resolve qmlls
        registerQmllsTask(project = project, toolingInfo = toolingInfo)
        // Try to resolve qmllint
        registerQmllintTask(project = project, toolingInfo = toolingInfo)

        // Ensure all tasks run after KSP has been triggered
        val kspTaskProvider = project.tasks.matching { it.name == "kspKotlin" }
            .takeIf { !it.isEmpty() }
            ?.let { project.tasks.named("kspKotlin") }

        kspTaskProvider?.configure {
            finalizedBy(qmlDirAndTypesTask, qmllsBuildIniTask, qmlToolingInfoTask)
        }

        // qmllint could be part of 'check' task, but currently example apps have too many warnings
        // project.tasks.matching { it.name == "check" }.configureEach { dependsOn(qmllintTask.name) }
    }

    private fun createQmlToolingInfo(
        project: Project,
        resolverFactory: QtResolverFactory,
        qmlExtension: QmlExtension
    ): QmlToolingInfo {

        val importPaths = buildList {
            add(project.appQmlImportDir().absolutePath)
            resolverFactory.qmlImportDirResolver.resolve()?.absolutePath?.let { add(it) }
            qmlExtension.resolveImportPaths().forEach { add(it.absolutePath) }
        }

        return QmlToolingInfo(
            qmllsPath = resolverFactory.qmllsResolver.resolve()?.absolutePath,
            qmllintPath = resolverFactory.qmllintResolver.resolve()?.absolutePath,
            qmllsIniFilePath = project.qmllsIniFile().absolutePath,
            buildDir = project.layout.buildDirectory.get().asFile.absolutePath,
            docDir = resolverFactory.qtDocDirResolver.resolve()?.absolutePath,
            qmlImportPaths = importPaths.distinct(),
        )
    }

    private fun createExtensions(project: Project): QtBridgeExtension {
        val extension = project.extensions.create("qtBridge", QtBridgeExtension::class.java, project)
        (extension as ExtensionAware).extensions.create("application", QtBridgeAppExtension::class.java, project)
        extension.extensions.create("qml", QmlExtension::class.java, project)
        return extension
    }

    private fun configureQtBridge(qtBridgeContext: QtBridgeContext) {
        val project = qtBridgeContext.project

        if (qtBridgeContext.isDevMode) {
            val buildNativeTask = qtBridgeContext.includedBridgeBuild!!.task(":generateLibsAndCopy")
            project.tasks.withType<JavaExec>().configureEach {
                dependsOn(buildNativeTask)
            }
        } else {
            val errorHandler = QtResourceExceptionHandler(project.logger)
            try {
                val qtResolverFactory = qtBridgeContext.qtResolverFactory
                val qtLibsDir = qtResolverFactory.libsLocationResolver.resolve()?.absolutePath.orEmpty()
                val qtBinDir = qtResolverFactory.qtBinDirResolver.resolve()?.absolutePath.orEmpty()
                val qtBridgeNativeDir = qtResolverFactory.bridgeNativeLocationResolver.resolve()?.absolutePath.orEmpty()

                configureTaskEnvironments(project, qtLibsDir, qtBinDir, qtBridgeNativeDir)
            } catch (e: Exception) {
                errorHandler.handle(e)
            }
        }
        // Configure Runtime environment
        val extension = qtBridgeContext.extension
        val appExtension = (extension as ExtensionAware).extensions.getByType(QtBridgeAppExtension::class.java)
        val qmlExtension = extension.extensions.getByType(QmlExtension::class.java)
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
        configureLogging(project)
        configureQmlFiles(project, qmlExtension)
        configureApplication(project, appExtension)
    }

    private fun configureLogging(project: Project) {
        // By default log "warning" category and above
        val logLevel = project.prop("qtbridge.log.level")
            ?.trim()?.lowercase()
            ?.takeIf{ it.isNotEmpty() }?: "warning"

        project.tasks.withType<JavaExec>().configureEach {
            // Map the log level to Qt logging categories. Qt logging category severities
            // are not hierarchical and hence we must enable/disable them individually
            val qtLoggingRules = when (logLevel) {
                // Disable all
                "off" -> "qtproject.qt.bridge*=false"
                // Enable critical
                "error", "severe", "critical" -> "qtproject.qt.bridge.critical=true;" +
                                                 "qtproject.qt.bridge.warning=false;" +
                                                 "qtproject.qt.bridge.debug=false"
                // Enable warning and critical
                "warn", "warning" -> "qtproject.qt.bridge.critical=true;" +
                                     "qtproject.qt.bridge.warning=true;" +
                                     "qtproject.qt.bridge.debug=false"
                // Enable debug, warning, and critical
                "debug", "info" -> "qtproject.qt.bridge.critical=true;" +
                                   "qtproject.qt.bridge.warning=true;" +
                                   "qtproject.qt.bridge.debug=true"
                // By default enable warning and critical
                else -> "qtproject.qt.bridge.critical=true;" +
                        "qtproject.qt.bridge.warning=true;" +
                        "qtproject.qt.bridge.debug=false"
            }
            // Qt logging categies use this environment variable
            environment("QT_LOGGING_RULES", qtLoggingRules)
            // Expose as system property for our Java logger initialization code
            systemProperty("qtbridge.log.level", logLevel)
        }
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

    private fun configureTaskEnvironments(
        project: Project,
        qtLibsDirPath: String,
        qtBinDirPath: String,
        qtBridgeLibraryPath: String
    ) {
        val environmentConfig = EnvironmentConfigurator(
            project.logger,
            qtLibsDirPath,
            qtBinDirPath,
            qtBridgeLibraryPath
        )
        project.tasks.withType(JavaExec::class.java).configureEach {
            environmentConfig.configure(this)
        }
    }

    private fun configureDependencies(project: Project) {
        val dependency = "org.qtproject.qt.bridge:qmlbridge:${QtBridgeResolverUtils.fullVersion()}"
        if (!project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) {
            project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        }
        if (!project.pluginManager.hasPlugin("com.google.devtools.ksp")) {
            project.pluginManager.apply("com.google.devtools.ksp")
        }
        project.dependencies.apply {
            add("implementation", dependency)
            add("ksp", dependency)
        }
    }

    private fun createRunTask(qtBridgeContext: QtBridgeContext) {
        val project = qtBridgeContext.project
        val appExtension =
            (qtBridgeContext.extension as ExtensionAware).extensions.getByType(QtBridgeAppExtension::class.java)
        if (appExtension.mainClass.isPresent) {
            val taskName = appExtension.name.getOrElse(project.name)

            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            val mainSourceSet = sourceSets.getByName(MAIN_SOURCE_SET_NAME)

            project.tasks.register(taskName, JavaExec::class.java) {
                group = "application"
                description = "Run the Qt Bridge ${project.name} name"
                classpath = mainSourceSet.runtimeClasspath
                mainClass.set(appExtension.mainClass)
            }
            project.logger.info("Created '$taskName' task for main class: ${appExtension.mainClass.get()}")
        }
    }

    private fun registerQmlToolingInfoForIDETask(
        project: Project,
        generateQmldirAndQmltypesTask: TaskProvider<Task>,
        qmllsBuildIniTask: TaskProvider<Task>,
        toolingInfo: QmlToolingInfo
    ): TaskProvider<Task> {
        return project.tasks.register("writeQmlToolingInfoForIDE") {
            group = "build"
            description = "Generates IDE-independent qmltools.json file for QML tooling support in IDEs."

            dependsOn(generateQmldirAndQmltypesTask, qmllsBuildIniTask)

            inputs.dir(project.appQmlImportDir())
                .withPropertyName("appQmlImportDir")
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .optional()

            val outputFile = QmlToolingInfo.getStandardFile(project.projectDir)
            outputs.file(outputFile)
                .withPropertyName("qmlToolsJson")

            doLast {
                toolingInfo.writeToFile(outputFile)

                project.logger.info("Wrote QML tooling info to: ${outputFile.absolutePath}")
                project.logger.info("IDE plugins should read this file to configure QML tooling support.")

                if (toolingInfo.qmllsPath == null) {
                    project.logger.warn("qmlls not found - IDE autocomplete may be limited. Install Qt 6.10+ for full support.")
                }
                if (toolingInfo.qmllintPath == null) {
                    project.logger.warn("qmllint not found - QML linting disabled.")
                }
            }
        }
    }

    private fun registerQmldirAndQmltypesTasks(project: Project, qmlRegistrar: String?): TaskProvider<Task> {
        // Only generate QML tooling files for 'main' source set. Generating for 'test' could
        // create a dependency loop: classes -> qmldirAndQmltypes-> ... -> kspTestKotlin -> classes
        // See QTBUG-143098 for processing more source sets.
        val sourceSetName = MAIN_SOURCE_SET_NAME

        // run qmltyperegistrar + write qmldir files
        return project.tasks.register("qtbridgeGenerateQmldirAndQmltypes") {
            group = "build"
            description = "Generates qmldir + .qmltypes for sourceSet $sourceSetName."

            // Ensure that 'generate' runs whenever moc files change, and that KSP has run
            inputs.dir(project.mocJsonDir())
                .withPropertyName("mocJsonDir")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            // Where we generate QML qmldir + plugins.qmltypes outputs
            outputs.dir(project.appQmlImportDir())

            val qmlTypeRegistrar = qmlRegistrar ?: run {
                project.logger.warn("Unable to resolve qmltyperegistrar. Lack of qmltyperegistrar impacts QML tooling " +
                        "support, but not the application compilation or run.")
                return@register
            }

            doLast {
                val importRoot = project.appQmlImportDir()
                // Get all <module>_moc.json files
                val mocFiles = project.mocFiles()

                if (mocFiles.isEmpty()) {
                    project.logger.info("No *_moc.json files found under ${project.mocJsonDir().absolutePath} " +
                            "for sourceSet $sourceSetName.")
                    return@doLast
                }

                mocFiles.map { mocFile ->
                    val uri = mocFile.name.toQmlModuleUri()
                    Triple(mocFile, uri, uri.toQmlModuleDir(importRoot))
                }.forEach { (mocFile, uri, moduleDir) ->
                    moduleDir.mkdirs()

                    val qmltypesFile = File(moduleDir, "plugins.qmltypes")
                    val qmldirFile = File(moduleDir, "qmldir")

                    // Generate .qmltypes (quiet by default, QTBUG-143092)
                    execOps.exec(object : Action<ExecSpec> {
                        override fun execute(spec: ExecSpec) {
                            spec.executable = qmlTypeRegistrar
                            spec.args(
                                "--generate-qmltypes", qmltypesFile.absolutePath,
                                "--import-name", uri,
                                "--major-version", "1",
                                "--minor-version", "0",
                                mocFile.absolutePath
                            )
                            // QTBUG-143092: allow output when possible. Currently qmltyperegistrar
                            // attempts to generate C++ registration code, which results in many warnings
                            spec.standardOutput = OutputStream.nullOutputStream()
                            spec.errorOutput = OutputStream.nullOutputStream()
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
    }

    // Sets up a task for writing .qmlls.build.ini file. qmlls from 6.10.0 reads
    // the .qmlls.build.ini file from the build folder to figure out import paths,
    // documentation paths, etc.
    private fun registerQmllsBuildIniTask(
        project: Project,
        generateQmldirAndQmltypesTask: TaskProvider<Task>,
        importPaths: List<String>,
        docDir: String?,
    ): TaskProvider<Task> {
        // QML source files
        val qmlSourceDir = project.projectDir
        return project.tasks.register("qtbridgeGenerateQmllsBuildIni") {
            group = "build"
            description = "Generates build/.qt/.qmlls.build.ini for qmlls (Qt 6.10+)."

            // Make sure the import tree / dependencies exists before writing ini
            dependsOn(generateQmldirAndQmltypesTask)

            // Depend on QML module contents
            inputs.dir(project.appQmlImportDir())
                .withPropertyName("appQmlImportRoot")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            // Where the .qmlls.build.ini goes (as expected by qmlls)
            outputs.file(project.qmllsIniFile())

            doLast {
                val iniFile = project.qmllsIniFile()
                iniFile.parentFile.mkdirs()

                val sectionName = qmlSourceDir.toqmllsSectionName()

                val text = buildString {
                    appendLine("[General]")
                    if (docDir != null) appendLine("docDir=$docDir")
                    appendLine()
                    appendLine("[$sectionName]")
                    appendLine("importPaths=\"${importPaths.joinToString(if (Platform.isWindows()) ";" else ":")}\"")
                    appendLine()
                }
                iniFile.writeText(text)
                project.logger.info("Wrote ${iniFile.absolutePath}. Run qmlls with: qmlls -b ${project.layout.buildDirectory.get().asFile.absolutePath}")
            }
        }
    }

    // Task for printing out the qmlls usage per application. Task is intended for
    // development and debugging purposes, as it prints the necessary command to run
    // (but other than that, running qmlls on command line is not all that useful)
    private fun registerQmllsTask(project: Project, toolingInfo: QmlToolingInfo): TaskProvider<Task> {
        return project.tasks.register("qtbridgeQmlls") {
            group = "verification"
            description = "Shows how to run qmlls against this build (requires Qt 6.10+)."
            val qmlls = toolingInfo.qmllsPath ?: run {
                project.logger.lifecycle("qmlls not found. `qtbridgeQmlls` cannot run.")
                return@register
            }

            dependsOn("qtbridgeGenerateQmllsBuildIni")
            doLast {
                val buildDir = toolingInfo.buildDir
                project.logger.lifecycle("qmlls found: $qmlls")
                project.logger.lifecycle("Run: $qmlls -b $buildDir")
            }
        }
    }

    // Task for testing the qmllinting, runs the qmllint for a given project. For instance:
    // ./gradlew :examples:manualtest:qtbridgeQmllint
    private fun registerQmllintTask(project: Project, toolingInfo: QmlToolingInfo): TaskProvider<Task> {
        return project.tasks.register("qtbridgeQmllint") {
            group = "verification"
            description = "Runs qmllint on QML sources using Qt Bridge generated import tree."
            val qmllint = toolingInfo.qmllintPath ?: run {
                project.logger.lifecycle("qmllint not found. `qtbridgeQmllint` cannot run.")
                return@register
            }

            dependsOn("qtbridgeGenerateQmldirAndQmltypes")
            inputs.dir(project.appQmlImportDir())
                .withPropertyName("appQmlImportRoot")
                .withPathSensitivity(PathSensitivity.RELATIVE)

            doLast {
                val qmlSourceDir = project.projectDir
                // Find the *.qml source files to lint
                val qmlFiles = project.fileTree(qmlSourceDir) { include("**/*.qml") }.files
                if (qmlFiles.isEmpty()) {
                    project.logger.lifecycle("No .qml files found under ${qmlSourceDir.absolutePath}")
                    return@doLast
                }

                qmlFiles.forEach { qmlFile ->
                    val args = ArrayList<String>()

                    // -I for each QML import path
                    toolingInfo.qmlImportPaths.forEach { importPath ->
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
    }
}
