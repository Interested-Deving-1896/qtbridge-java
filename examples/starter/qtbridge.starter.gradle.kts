/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

import java.io.File

// Usage:
//   gradle --init-script qtbridge.gradle.kts
//          -PprojectName=MyApp
//          -PpackageName=com.example
//          -Planguage=kotlin

data class ProjectConfig(
    val projectName: String,
    val packageName: String,
    val language: String,
    val qtBridgeVersion: String = "0.1.0"
)

object TemplateProvider {
    fun getMainFile(packageName: String, language: String) =
        if (language.equals("java", ignoreCase = true)) {
            """
                package $packageName;

                import org.qtproject.qt.bridge.core.QtQuickApplication;

                public class Main {
                    public static void main(String[] args) {
                        new QtQuickApplication(args).execute();
                    }
                }
            """.trimIndent()
        } else {
            """
                package $packageName

                import org.qtproject.qt.bridge.core.QtQuickApplication

                fun main(args: Array<String>) {
                    QtQuickApplication(args).execute()
                }
            """.trimIndent()
        }

    fun getControllerFile(packageName: String, language: String) =
        if (language.equals("java", ignoreCase = true)) {
            """
                package $packageName;

                import org.qtproject.qt.bridge.annotations.QMLRegistrable;
                import org.qtproject.qt.bridge.core.QtProperty;

                @QMLRegistrable(singleton = true)
                public class Controller {

                    public final QtProperty<String> text = new QtProperty<>("Hello QtBridge!");
                    public final QtProperty<String> color = new QtProperty<>("#2196F3");

                    public void clickMe() {
                        text.setValue("Button clicked!");
                        color.setValue("#4CAF50");
                    }

                    public void reset() {
                        text.setValue("Hello QtBridge!");
                        color.setValue("#2196F3");
                    }
                }
            """.trimIndent()
        } else {
            """
                package $packageName

                import org.qtproject.qt.bridge.annotations.QMLRegistrable
                import org.qtproject.qt.bridge.core.QtProperty

                @QMLRegistrable(singleton = true)
                class Controller {

                    val text = QtProperty("Hello QtBridge!")
                    val color = QtProperty("#2196F3")

                    fun clickMe() {
                        text.value = "Button clicked!"
                        color.value = "#4CAF50"
                    }

                    fun reset() {
                        text.value = "Hello QtBridge!"
                        color.value = "#2196F3"
                    }
                }
            """.trimIndent()
        }

    fun getBuildFile(
        packageName: String,
        qtBridgeVersion: String,
        projectName: String,
        language: String
    ): String = buildString {
        val suffix = if (language.equals("java", ignoreCase = true)) "" else "Kt"

        appendLine("plugins {")
        if (language.equals("java", ignoreCase = true)) {
            appendLine("""    id("java")""")
        }
        appendLine("""    id("org.qtproject.qt.bridge.qtbridge-plugin") version "$qtBridgeVersion"""")
        appendLine("}")
        appendLine()
        appendLine("""group = "$packageName"""")
        appendLine("""version = "1.0-SNAPSHOT"""")
        appendLine()
        appendLine("qtBridge {")
        appendLine("    application {")
        appendLine("""        name = "$projectName"""")
        appendLine("""        mainClass = "$packageName.Main$suffix"""")
        appendLine("    }")
        append("}")
    }

    fun getSettingsFile(projectName: String) =
        """
        pluginManagement {
            repositories {
                gradlePluginPortal()
                maven { url = uri("https://android.qt.io/maven/releases") }
            }
        }
        rootProject.name = "$projectName"
        """.trimIndent()

    fun getQmlFile() =
        """
            import QtQuick
            import QtQuick.Controls.Basic
            import QtQuick.Layouts
            import QtBridge

            Window {
                id: window
                width: 400
                height: 300
                visible: true
                title: qsTr("QtBridge App")
                color: "#f5f5f5"

                ColumnLayout {
                    anchors.centerIn: parent
                    spacing: 20

                    Text {
                        text: Controller.text
                        font.pixelSize: 32
                        font.bold: true
                        color: Controller.color
                        horizontalAlignment: Text.AlignHCenter
                        Layout.alignment: Qt.AlignHCenter
                    }

                    Button {
                        text: qsTr("Click Me!")
                        font.pixelSize: 16
                        Layout.alignment: Qt.AlignHCenter
                        Layout.preferredWidth: 150
                        Layout.preferredHeight: 40

                        background: Rectangle {
                            color: parent.pressed ? "#1976D2"
                                  : parent.hovered ? "#42A5F5"
                                  : "#2196F3"
                            radius: 6
                        }

                        contentItem: Text {
                            text: parent.text
                            font: parent.font
                            color: "white"
                            horizontalAlignment: Text.AlignHCenter
                            verticalAlignment: Text.AlignVCenter
                        }

                        onClicked: Controller.clickMe()
                    }

                    Button {
                        text: qsTr("Reset")
                        font.pixelSize: 14
                        Layout.alignment: Qt.AlignHCenter
                        Layout.preferredWidth: 150
                        Layout.preferredHeight: 36

                        background: Rectangle {
                            color: parent.pressed ? "#616161"
                                  : parent.hovered ? "#9E9E9E"
                                  : "#757575"
                            radius: 6
                        }

                        contentItem: Text {
                            text: parent.text
                            font: parent.font
                            color: "white"
                            horizontalAlignment: Text.AlignHCenter
                            verticalAlignment: Text.AlignVCenter
                        }

                        onClicked: Controller.reset()
                    }
                }
            }
        """.trimIndent()
}

//Project initialization
val invokedFromDir = File(System.getProperty("user.dir"))

gradle.rootProject {
    val genTask = tasks.register("generateQtBridgeProject") {
        group = "setup"
        description = "Generates a new QtBridge project structure"
        doLast {
            val invokedDir = invokedFromDir.canonicalFile
            val projectName = gradle.startParameter.projectProperties["projectName"] ?: "MyApp"
            val rootDir = File(invokedDir, projectName).apply {
                if (!exists() && !mkdirs()) error("Failed to create project directory: $this")
            }

            println("\n" + "=".repeat(90))
            println("Starting project initialization in '$rootDir'...\n")

            val getProp = { key: String, default: String ->
                gradle.startParameter.projectProperties[key] ?: default
            }

            val config = ProjectConfig(
                projectName = projectName,
                packageName = getProp("packageName", "com.example"),
                language = getProp("language", "java").lowercase(),
            )

            println("Project Configuration:")
            println("  Name:    ${config.projectName}")
            println("  Package: ${config.packageName}")
            println("  Lang:    ${config.language}")
            println("  QtBridge Version: ${config.qtBridgeVersion}\n")

            fun ensureDir(path: String) = File(rootDir, path).apply {
                if (!exists() && !mkdirs()) error("Failed to create directory: $this")
            }

            fun writeIfMissing(file: File, content: String) {
                if (!file.exists()) {
                    file.writeText(content)
                    println("Created ${file.path}")
                }
            }

            writeIfMissing(File(rootDir, "settings.gradle.kts"), TemplateProvider.getSettingsFile(config.projectName))
            writeIfMissing(
                File(rootDir, "build.gradle.kts"),
                TemplateProvider.getBuildFile(
                    config.packageName,
                    config.qtBridgeVersion,
                    config.projectName,
                    config.language
                )
            )

            val srcFolder = if (config.language == "java") "java" else "kotlin"
            val packagePath = config.packageName.replace('.', '/')
            val mainDir = ensureDir("src/main/$srcFolder/$packagePath")

            writeIfMissing(
                File(mainDir, "Main.${if (config.language == "java") "java" else "kt"}"),
                TemplateProvider.getMainFile(config.packageName, config.language)
            )
            writeIfMissing(
                File(mainDir, "Controller.${if (config.language == "java") "java" else "kt"}"),
                TemplateProvider.getControllerFile(config.packageName, config.language)
            )

            val qmlDir = ensureDir("src/main/qml")
            writeIfMissing(File(qmlDir, "main.qml"), TemplateProvider.getQmlFile())

            println("\nProject initialized successfully!")
            println("Now run:")
            println("  cd ${config.projectName} && gradle ${config.projectName}\n")
            println("=".repeat(90))
        }
    }
    defaultTasks(genTask.name)
    tasks.register("usage") {
        group = "help"
        doLast {
            println("\n" + "=".repeat(90))
            println("QtBridge Project Generator - Usage")
            println("=".repeat(90))
            println("\nUSAGE:")
            println("  gradle --init-script qtbridge.gradle.kts [task] [parameters]")
            println("\nTASKS:")
            println("  generateQtBridgeProject   : Generates the project (Default)")
            println("  usage                      : Displays this message")
            println("\nPARAMETERS:")
            println("  -PprojectName=<name>      : Name of the project (default: MyApp)")
            println("  -PpackageName=<package>   : Java/Kotlin package name (default: com.example)")
            println("  -Planguage=<lang>         : 'java' or 'kotlin' (default: java)")
            println("\nEXAMPLES:")
            println("  # Run Generator:")
            println("  gradle --init-script qtbridge.gradle.kts")
            println("\n  # View Help:")
            println("  gradle --init-script qtbridge.gradle.kts usage")
            println("\n  # Custom Generation:")
            println("  gradle --init-script qtbridge.gradle.kts -PprojectName=MyQtApp -Planguage=kotlin")
            println("\n" + "=".repeat(90) + "\n")
        }
    }
}
