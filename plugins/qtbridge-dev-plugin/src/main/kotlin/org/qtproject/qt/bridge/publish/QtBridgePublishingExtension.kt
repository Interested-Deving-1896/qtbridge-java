/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

open class QtBridgePublishingExtension(project: Project) {
    val artifactId = project.objects.property(String::class.java).convention(project.name)
    val moduleName = project.objects.property(String::class.java)
    val moduleDescription = project.objects.property(String::class.java)
    val organization = project.objects.property(String::class.java).convention("Qt Group")
    val developerUrl = project.objects.property(String::class.java).convention("https://www.qt.io")
    val developerEmail = project.objects.property(String::class.java).convention("qtbridges@qt.io")
    // TODO: QTBUG:142220 check urls when are available
    val repositoryUrl = project.objects.property(String::class.java).convention("https://code.qt.io/cgit/qt-labs/qtbridge-java")
    val gitUrl = project.objects.property(String::class.java).convention("git://code.qt.io/qt-labs/qtbridge-java")
    val codeReviewUrl = project.objects.property(String::class.java).convention("https://codereview.qt-project.org/qt-labs/qtbridge-java")
    val licenses = project.objects.domainObjectContainer(LicenseSpec::class.java) { LicenseSpec(it, project.objects) }

    open class LicenseSpec(val licenseSpecName: String, objects: ObjectFactory) : Named {
        val licenseName = objects.property(String::class.java)
        val licenseUrl = objects.property(String::class.java)
        override fun getName(): String = licenseSpecName
    }

    init {
        licenses.create("LicenseRef-Qt-Commercial") {
            licenseName.set("LicenseRef-Qt-Commercial")
            licenseUrl.set("https://www.qt.io/terms-conditions")
        }
        licenses.create("LGPL-3.0-only") {
            licenseName.set("LGPL-3.0-only")
            licenseUrl.set("https://www.gnu.org/licenses/")
        }
    }
}
