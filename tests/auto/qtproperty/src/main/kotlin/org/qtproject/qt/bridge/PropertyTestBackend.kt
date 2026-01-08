/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.core.QtProperty
import java.net.URI

@QMLRegistrable(name = "KotlinTestBackend", singleton = true)
class PropertyTestBackend {
    val mapProperty = QtProperty<Map<String, Any?>?>(null);

    fun setObjectMapPublicOnly() {
        mapProperty.setValue(QtProperty.toMap(ChildClass()))
    }

    fun setObjectMapIncludeNonPublic() {
        mapProperty.setValue(QtProperty.toMap(ChildClass(), true))
    }

    fun setNullObjectMap() {
        mapProperty.setValue(null)
    }

    enum class Color(val rgb: String) {
        RED("#FF0000"), GREEN("#00FF00"), BLUE("#0000FF")
    }

    data class NestedClass(
        val nestedString: String = "€€nested",
        val nestedInt: Int = 123
    )

    open class ParentClass(
        val parentInt: Int = 12,
        val parentString: String = "€€parent"
    ) {
        protected val parentProtectedString: String = "parentProtected"
    }

    data class ChildClass(
        val childint: Int = 7,
        val childfloat: Float = 3.14f,
        val childdouble: Double = 4.14,
        val childshort: Short = 2,
        val childboolean: Boolean = true,
        val childlong: Long = 9L,
        val childbyte: Byte = 11,
        val childchar: Char = 'a',
        val childString: String = "€€child",
        val childURI: URI = URI.create("https://example.com"),
        val childList: List<Any> = listOf("tag", 1, true, NestedClass()),
        val childMap: Map<String, Any> = mapOf("k1" to "v1", "k2" to 2, "k3" to NestedClass()),
        val childEnum: Color = Color.RED,
        val childNestedClass: NestedClass = NestedClass(),
        // Boolean with "isXxx" getter to test bean-style boolean naming
        val isChildBoolean: Boolean = true,
        val childBoolean: Boolean = true,
        private val childPrivateString: String = "shh"
    ) : ParentClass()
}
