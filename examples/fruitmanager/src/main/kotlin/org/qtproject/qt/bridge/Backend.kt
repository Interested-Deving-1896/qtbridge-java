/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.core.QtProperty
import org.qtproject.qt.bridge.core.QtListModel
import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals

@QMLRegistrable(name = "Backend", singleton = true)
class Backend(fruitsProvider: FruitsProvider = FruitsProvider()) {
    @QMLSignals
    private var backendCallback: BackendCallback? = null

    // Accessed in QML
    val colorOptions = QtProperty(FruitColor.entries.map { it.name }.toList())
    val categoryOptions = QtProperty(FruitCategory.entries.map { it.name }.toList())
    val fruitList = QtListModel(fruitsProvider.fetchAll());

    fun addFruitWithValidation(
        fruitName: String,
        color: String,
        category: String,
        isTropical: Boolean,
        calories: Int,
        weight: Double
    ) {
        try {
            if (fruitName.isBlank()) {
                backendCallback?.validationError("Name is required.")
                return
            }

            if (calories < 0) {
                backendCallback?.validationError("Calories must be a non-negative number.")
                return
            }
            if (calories == 0) {
                backendCallback?.validationError("Calories must be larger than zero.")
                return
            }

            if (weight < 0) {
                backendCallback?.validationError("Weight must be a positive number.")
                return
            }

            if (weight == 0.0) {
                backendCallback?.validationError("Weight must be larger than zero.")
                return
            }

            val fruitColor = try {
                FruitColor.valueOf(color)
            } catch (e: IllegalArgumentException) {
                backendCallback?.validationError("Invalid color: $color")
                return
            }

            val fruitCategory = try {
                FruitCategory.valueOf(category)
            } catch (e: IllegalArgumentException) {
                backendCallback?.validationError("Invalid category: $category")
                return
            }

            val fruit = Fruit(
                fruitName,
                fruitColor,
                fruitCategory,
                isTropical,
                Nutrition(calories, weight)
            )

            if (fruitList.contains(fruit)) {
                backendCallback?.duplicateError(fruit.name)
                return
            }

            fruitList.appendItem(fruit)
            backendCallback?.fruitAdded(fruit.name)

        } catch (e: Exception) {
            backendCallback?.operationError("Failed to add fruit: ${e.message}")
        }
    }

    fun updateFruitWithValidation(
        index: Int,
        updatedFruitName: String,
        color: String,
        category: String,
        isTropical: Boolean,
        calories: Int,
        weight: Double
    ) {
        try {
            if (index < 0 || index >= fruitList.size()) {
                backendCallback?.validationError("Invalid fruit index.")
                return
            }

            if (updatedFruitName.isBlank()) {
                backendCallback?.validationError("Name is required.")
                return
            }

            if (calories < 0) {
                backendCallback?.validationError("Calories must be a non-negative number.")
                return
            }

            if (weight <= 0) {
                backendCallback?.validationError("Weight must be a positive number.")
                return
            }

            val fruitColor = try {
                FruitColor.valueOf(color)
            } catch (e: IllegalArgumentException) {
                backendCallback?.validationError("Invalid color: $color")
                return
            }

            val fruitCategory = try {
                FruitCategory.valueOf(category)
            } catch (e: IllegalArgumentException) {
                backendCallback?.validationError("Invalid category: $category")
                return
            }

            val updatedFruit = Fruit(
                updatedFruitName,
                fruitColor,
                fruitCategory,
                isTropical,
                Nutrition(calories, weight)
            )

            if (fruitList.contains(updatedFruit)) {
                backendCallback?.fruitUpdated(updatedFruit.name)
                return
            }

            fruitList.updateItemAt(index, updatedFruit)
            backendCallback?.fruitUpdated(updatedFruit.name)

        } catch (e: Exception) {
            backendCallback?.operationError("Failed to update fruit: ${e.message}")
        }
    }

    fun deleteFruit(index: Int) {
        try {
            // Validate index
            if (index < 0 || index >= fruitList.size()) {
                backendCallback?.validationError("Invalid fruit index.")
                return
            }

            fruitList.removeItemAt(index)
            backendCallback?.fruitDeleted("fruitName")

        } catch (e: Exception) {
            backendCallback?.operationError("Failed to delete fruit: ${e.message}")
        }
    }
}
