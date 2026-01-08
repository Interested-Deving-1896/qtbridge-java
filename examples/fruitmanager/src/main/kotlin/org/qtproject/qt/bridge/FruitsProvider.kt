/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

class FruitsProvider {
    private val dataList = mutableListOf(
        Fruit("Apple", FruitColor.RED, FruitCategory.OTHER, false, Nutrition(52, 182.0)),
        Fruit("Banana", FruitColor.YELLOW, FruitCategory.OTHER, true, Nutrition(89, 118.0)),
        Fruit("Cherry", FruitColor.RED, FruitCategory.STONE_FRUIT, false, Nutrition(50, 8.0)),
        Fruit("Date", FruitColor.BROWN, FruitCategory.OTHER, true, Nutrition(282, 7.1)),
        Fruit("Elderberry", FruitColor.PURPLE, FruitCategory.BERRY, false, Nutrition(73, 5.0)),
        Fruit("Fig", FruitColor.PURPLE, FruitCategory.OTHER, true, Nutrition(74, 50.0)),
        Fruit("Grapes", FruitColor.GREEN, FruitCategory.BERRY, false, Nutrition(69, 5.0)),
        Fruit("Honeydew", FruitColor.GREEN, FruitCategory.MELON, false, Nutrition(36, 1280.0)),
        Fruit("Kiwi", FruitColor.BROWN, FruitCategory.OTHER, true, Nutrition(61, 76.0)),
        Fruit("Lemon", FruitColor.YELLOW, FruitCategory.CITRUS, false, Nutrition(29, 58.0)),
        Fruit("Lime", FruitColor.GREEN, FruitCategory.CITRUS, false, Nutrition(30, 67.0)),
        Fruit("Mango", FruitColor.ORANGE, FruitCategory.OTHER, true, Nutrition(60, 200.0)),
        Fruit("Nectarine", FruitColor.ORANGE, FruitCategory.STONE_FRUIT, false, Nutrition(44, 150.0)),
        Fruit("Orange", FruitColor.ORANGE, FruitCategory.CITRUS, false, Nutrition(47, 131.0)),
        Fruit("Papaya", FruitColor.ORANGE, FruitCategory.OTHER, true, Nutrition(43, 500.0)),
        Fruit("Peach", FruitColor.ORANGE, FruitCategory.STONE_FRUIT, false, Nutrition(39, 150.0)),
        Fruit("Pear", FruitColor.GREEN, FruitCategory.OTHER, false, Nutrition(57, 178.0)),
        Fruit("Pineapple", FruitColor.YELLOW, FruitCategory.OTHER, true, Nutrition(50, 905.0)),
        Fruit("Plum", FruitColor.PURPLE, FruitCategory.STONE_FRUIT, false, Nutrition(46, 66.0)),
        Fruit("Pomegranate", FruitColor.RED, FruitCategory.OTHER, false, Nutrition(83, 282.0)),
        Fruit("Raspberry", FruitColor.RED, FruitCategory.BERRY, false, Nutrition(52, 4.0)),
        Fruit("Strawberry", FruitColor.RED, FruitCategory.BERRY, false, Nutrition(33, 12.0)),
        Fruit("Tangerine", FruitColor.ORANGE, FruitCategory.CITRUS, true, Nutrition(53, 88.0)),
        Fruit("Ugli Fruit", FruitColor.GREEN, FruitCategory.CITRUS, true, Nutrition(45, 150.0)),
        Fruit("Watermelon", FruitColor.GREEN, FruitCategory.MELON, true, Nutrition(30, 9200.0)),
        Fruit("Blueberry", FruitColor.BLUE, FruitCategory.BERRY, false, Nutrition(57, 1.0)),
        Fruit("Blackberry", FruitColor.BLACK, FruitCategory.BERRY, false, Nutrition(43, 5.0)),
        Fruit("Cantaloupe", FruitColor.ORANGE, FruitCategory.MELON, false, Nutrition(34, 1000.0)),
        Fruit("Coconut", FruitColor.BROWN, FruitCategory.OTHER, true, Nutrition(354, 1500.0)),
        Fruit("Grapefruit", FruitColor.PINK, FruitCategory.CITRUS, false, Nutrition(42, 230.0)),
        Fruit("Guava", FruitColor.GREEN, FruitCategory.OTHER, true, Nutrition(68, 55.0)),
        Fruit("Lychee", FruitColor.RED, FruitCategory.OTHER, true, Nutrition(66, 20.0)),
        Fruit("Passionfruit", FruitColor.PURPLE, FruitCategory.OTHER, true, Nutrition(97, 18.0)),
        Fruit("Rambutan", FruitColor.RED, FruitCategory.OTHER, true, Nutrition(68, 35.0)),
        Fruit("Dragonfruit", FruitColor.PINK, FruitCategory.OTHER, true, Nutrition(50, 600.0))
    )
    fun fetchAll() = dataList.toList()
}
