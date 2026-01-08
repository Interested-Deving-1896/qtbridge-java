/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension
import org.qtproject.qt.bridge.publish.PublishContext
import org.qtproject.qt.bridge.publish.PublishEnvironment

internal class SigningConfigurer : Configurer {

    override fun configure(context: PublishContext) {
        val project = context.project
        val environment = context.environment
        if (environment == PublishEnvironment.LOCAL) {
            return
        }
        project.extensions.configure<SigningExtension> {
            configureKeys(project)
            signPublication(project)
        }
    }

    private fun SigningExtension.configureKeys(project: Project) {
        val signingKey = project.findProperty("signing.key") as String?
        val signingPassword = project.findProperty("signing.password") as String?

        if (signingKey.isNullOrBlank() || signingPassword.isNullOrBlank()) {
            throw GradleException("Signing keys are missing. You must provide both 'signing.key' and 'signing.password'.")
        }
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    private fun SigningExtension.signPublication(project: Project) {
        val publishingExtension = project.extensions.getByType<PublishingExtension>()
        sign(publishingExtension.publications)
    }
}
