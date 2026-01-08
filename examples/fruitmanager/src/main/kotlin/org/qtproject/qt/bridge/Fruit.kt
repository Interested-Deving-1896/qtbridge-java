/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

enum class FruitColor(val hex: String) {
    RED("#E53935"),
    ORANGE("#FB8C00"),
    YELLOW("#FDD835"),
    GREEN("#43A047"),
    BLUE("#1E88E5"),
    PURPLE("#8E24AA"),
    BROWN("#795548"),
    WHITE("#CFD8DC"),
    BLACK("#263238"),
    PINK("#EC407A")
}

enum class FruitCategory {
    CITRUS, BERRY, MELON, OTHER, STONE_FRUIT
}

data class Fruit(
    val name: String,
    val color: FruitColor,
    val category: FruitCategory = FruitCategory.OTHER,
    val isTropical: Boolean = false,
    val nutrition: Nutrition
)

data class Nutrition(val calories: Int, val weight: Double)
