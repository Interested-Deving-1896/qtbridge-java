/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

class Repository {
    private val dataList = mutableListOf(
        "Apple",
        "Banana",
        "Cherry",
        "Date",
        "Elderberry",
        "Fig",
        "Grapes",
        "Honeydew",
        "Kiwi",
        "Lemon",
        "Mango",
        "Nectarine",
        "Orange",
        "Papaya",
        "Quince",
        "Raspberry",
        "Strawberry",
        "Tangerine",
        "Ugli Fruit",
        "Watermelon"
    )

    fun fetchAll() = dataList.toList()

    fun addItem(item: String): Boolean {
        return if (item.isNotBlank() && !dataList.contains(item)) {
            dataList.add(item)
            true
        } else {
            false
        }
    }

    fun removeItem(item: String) = dataList.remove(item)

    fun updateItem(oldItem: String, newItem: String): Boolean {
        val index = dataList.indexOf(oldItem)
        return if (index != -1 && newItem.isNotBlank()) {
            dataList[index] = newItem
            true
        } else {
            false
        }
    }

    fun searchItems(query: String) = dataList.filter { it.contains(query, ignoreCase = true) }

    fun count() = dataList.size
}
